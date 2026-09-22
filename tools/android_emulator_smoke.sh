#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
package_name="${ANDROID_PACKAGE_NAME:-com.jjswigut.oopsallprs.android.debug}"
activity_name="${ANDROID_ACTIVITY_NAME:-com.jjswigut.oopsallprs.MainActivity}"
adb_bin="${ADB:-}"
emulator_bin="${EMULATOR:-}"
avd_name="${ANDROID_AVD_NAME:-}"
requested_device="${ANDROID_DEVICE_SERIAL:-}"
artifact_dir="${SMOKE_ARTIFACT_DIR:-$repo_root/build/smoke/android}"
launch_settle_seconds="${ANDROID_LAUNCH_SETTLE_SECONDS:-3}"
emulator_boot_attempts="${ANDROID_EMULATOR_BOOT_ATTEMPTS:-60}"
emulator_boot_delay_seconds="${ANDROID_EMULATOR_BOOT_DELAY_SECONDS:-5}"
emulator_log="$artifact_dir/emulator.log"
started_emulator_pid=""

cd "$repo_root"

if [ -z "$adb_bin" ]; then
  for candidate in \
    "${ANDROID_HOME:-}/platform-tools/adb" \
    "${ANDROID_SDK_ROOT:-}/platform-tools/adb" \
    "$HOME/Library/Android/sdk/platform-tools/adb" \
    adb; do
    if [ -n "$candidate" ] && command -v "$candidate" >/dev/null 2>&1; then
      adb_bin="$candidate"
      break
    fi
  done
fi

if [ -z "$adb_bin" ] || ! command -v "$adb_bin" >/dev/null 2>&1; then
  echo "adb is not available. Start Android Studio tooling or set ADB=/path/to/adb." >&2
  exit 2
fi

if [ -z "$emulator_bin" ]; then
  for candidate in \
    "${ANDROID_HOME:-}/emulator/emulator" \
    "${ANDROID_SDK_ROOT:-}/emulator/emulator" \
    "$HOME/Library/Android/sdk/emulator/emulator" \
    emulator; do
    if [ -n "$candidate" ] && command -v "$candidate" >/dev/null 2>&1; then
      emulator_bin="$candidate"
      break
    fi
  done
fi

"$adb_bin" start-server >/dev/null

cleanup() {
  local status="$?"
  if [ -n "$started_emulator_pid" ] && [ "${ANDROID_SHUTDOWN_STARTED_EMULATOR:-0}" = "1" ]; then
    if [ -n "${device:-}" ]; then
      "$adb_bin" -s "$device" emu kill >/dev/null 2>&1 || true
    fi
    kill "$started_emulator_pid" 2>/dev/null || true
    # The macOS emulator launcher can fork QEMU and exit before cleanup runs.
    # Target only the AVD this invocation started, never a connected device.
    for qemu_pid in $(pgrep -f "qemu-system.*-avd $avd_name" 2>/dev/null || true); do
      kill "$qemu_pid" 2>/dev/null || true
    done
  fi
  exit "$status"
}

trap cleanup EXIT

find_online_device() {
  if [ -n "$requested_device" ]; then
    "$adb_bin" devices | awk -v serial="$requested_device" 'NR > 1 && $1 == serial && $2 == "device" { print $1; exit }'
    return
  fi
  "$adb_bin" devices \
    | awk 'NR > 1 && $1 ~ /^emulator-/ && $2 == "device" { print $1; exit }'
}

device="$(find_online_device)"

if [ -z "$device" ] && [ "${ANDROID_START_EMULATOR:-1}" != "0" ]; then
  if [ -z "$emulator_bin" ] || ! command -v "$emulator_bin" >/dev/null 2>&1; then
    echo "No online Android device found, and emulator is not available. Set EMULATOR=/path/to/emulator or start a device manually." >&2
    exit 3
  fi

  if [ -z "$avd_name" ]; then
    avd_name="$("$emulator_bin" -list-avds | sed -n '1p')"
  fi

  if [ -z "$avd_name" ]; then
    echo "No online Android device found, and no Android Virtual Devices are configured." >&2
    exit 3
  fi

  mkdir -p "$artifact_dir"
  echo "Starting Android emulator: $avd_name"
  # shellcheck disable=SC2086
  "$emulator_bin" -avd "$avd_name" ${ANDROID_EMULATOR_ARGS:--no-snapshot-save -no-boot-anim} > "$emulator_log" 2>&1 &
  started_emulator_pid="$!"

  for _ in $(seq 1 "$emulator_boot_attempts"); do
    device="$(find_online_device)"
    if [ -n "$device" ]; then
      boot_completed="$("$adb_bin" -s "$device" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
      if [ "$boot_completed" = "1" ]; then
        break
      fi
    fi
    sleep "$emulator_boot_delay_seconds"
  done
else
  :
fi

if [ -z "$device" ]; then
  cat >&2 <<'EOF'
No online Android device or emulator found.

Start an emulator from Android Studio or with:
  emulator -avd <name>

Then rerun:
  tools/android_emulator_smoke.sh
EOF
  exit 3
fi

boot_completed="$("$adb_bin" -s "$device" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
if [ "$boot_completed" != "1" ]; then
  echo "Android device '$device' did not finish booting. Emulator log: $emulator_log" >&2
  exit 6
fi

mkdir -p "$artifact_dir"

./gradlew --no-daemon :androidApp:assembleDebug

apk_path="$repo_root/androidApp/build/outputs/apk/debug/androidApp-debug.apk"
test -f "$apk_path" || {
  echo "Expected APK not found: $apk_path" >&2
  exit 4
}

"$adb_bin" -s "$device" install -r "$apk_path" >/dev/null
"$adb_bin" -s "$device" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
"$adb_bin" -s "$device" shell wm dismiss-keyguard >/dev/null 2>&1 || true
"$adb_bin" -s "$device" shell input keyevent KEYCODE_MENU >/dev/null 2>&1 || true
"$adb_bin" -s "$device" shell am force-stop "$package_name" >/dev/null || true
if ! "$adb_bin" -s "$device" shell am start -n "$package_name/$activity_name" >/dev/null 2>&1; then
  "$adb_bin" -s "$device" shell monkey -p "$package_name" -c android.intent.category.LAUNCHER 1 >/dev/null || true
fi

foreground_path="$artifact_dir/foreground-activity.txt"
foreground=0
for _ in $(seq 1 10); do
  "$adb_bin" -s "$device" shell dumpsys activity activities > "$foreground_path" || true
  if rg -F 'mCurrentFocus=' "$foreground_path" | rg -F -q "$package_name" &&
    ! rg -q 'isSleeping=true' "$foreground_path"; then
    foreground=1
    break
  fi
  sleep 1
done

if [ "$foreground" -ne 1 ]; then
  "$adb_bin" -s "$device" logcat -d -t 300 > "$artifact_dir/logcat-tail.txt" || true
  echo "App did not become an awake foreground activity. Activity state: $foreground_path; log tail: $artifact_dir/logcat-tail.txt" >&2
  exit 5
fi

pid="$("$adb_bin" -s "$device" shell pidof "$package_name" | tr -d '\r' || true)"
if [ -z "$pid" ]; then
  "$adb_bin" -s "$device" logcat -d -t 300 > "$artifact_dir/logcat-tail.txt" || true
  echo "App did not stay running after launch. Log tail: $artifact_dir/logcat-tail.txt" >&2
  exit 5
fi

# A foreground activity can still be displaying Android's starting window.
sleep "$launch_settle_seconds"

screenshot_path="$artifact_dir/launch.png"
"$adb_bin" -s "$device" exec-out screencap -p > "$screenshot_path" || true

cat <<EOF
Android smoke passed.
Device: $device
Package: $package_name
Activity: $activity_name
PID: $pid
Screenshot: $screenshot_path
Foreground window: $foreground_path
EOF
