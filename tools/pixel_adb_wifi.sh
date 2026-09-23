#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="${PIXEL_ADB_CONFIG:-"$ROOT_DIR/.env.pixel-adb"}"
APP_ID_DEFAULT="com.jjswigut.oopsallprs.android.debug"
INSTALL_TASK_DEFAULT=":androidApp:installDebug"

usage() {
  cat <<'USAGE'
Usage:
  tools/pixel_adb_wifi.sh pair <pair-ip:port> <pair-code> [connect-ip:port]
  tools/pixel_adb_wifi.sh connect [connect-ip:port]
  tools/pixel_adb_wifi.sh install [connect-ip:port]
  tools/pixel_adb_wifi.sh launch [connect-ip:port]
  tools/pixel_adb_wifi.sh deploy [connect-ip:port]
  tools/pixel_adb_wifi.sh status

Pixel setup:
  Settings > System > Developer options > Wireless debugging

Notes:
  - Pairing is usually one-time per Mac/Pixel authorization.
  - The pair-ip:port comes from "Pair device with pairing code".
  - The connect-ip:port is the main Wireless debugging address shown after pairing.
  - With no address, the script discovers an already-paired device through ADB mDNS.
  - The last successful connect-ip:port is a fallback saved in .env.pixel-adb, which is ignored by .gitignore.

Environment overrides:
  ADB=/path/to/adb
  PIXEL_ADB_CONFIG=/path/to/config
  PIXEL_APP_ID=com.jjswigut.oopsallprs.android.debug
  PIXEL_INSTALL_TASK=:androidApp:installDebug
USAGE
}

find_adb() {
  if [[ -n "${ADB:-}" && -x "${ADB:-}" ]]; then
    printf '%s\n' "$ADB"
    return
  fi

  local path_adb
  path_adb="$(command -v adb 2>/dev/null || true)"
  if [[ -n "$path_adb" && -x "$path_adb" ]]; then
    printf '%s\n' "$path_adb"
    return
  fi

  local candidates=(
    "${ANDROID_HOME:-}/platform-tools/adb"
    "${ANDROID_SDK_ROOT:-}/platform-tools/adb"
    "$HOME/Library/Android/sdk/platform-tools/adb"
  )
  local candidate
  for candidate in "${candidates[@]}"; do
    if [[ -n "$candidate" && -x "$candidate" ]]; then
      printf '%s\n' "$candidate"
      return
    fi
  done

  printf 'Unable to find adb. Set ADB=/path/to/adb or install Android platform-tools.\n' >&2
  exit 1
}

load_config() {
  if [[ -f "$CONFIG_FILE" ]]; then
    # shellcheck disable=SC1090
    source "$CONFIG_FILE"
  fi
  APP_ID="${PIXEL_APP_ID:-${APP_ID:-$APP_ID_DEFAULT}}"
  INSTALL_TASK="${PIXEL_INSTALL_TASK:-${PIXEL_INSTALL_TASK_VALUE:-$INSTALL_TASK_DEFAULT}}"
}

save_serial() {
  local serial="$1"
  local tmp_file="$CONFIG_FILE.tmp"
  {
    printf 'ADB_WIFI_SERIAL=%q\n' "$serial"
    printf 'PIXEL_APP_ID=%q\n' "$APP_ID"
    printf 'PIXEL_INSTALL_TASK_VALUE=%q\n' "$INSTALL_TASK"
  } > "$tmp_file"
  mv "$tmp_file" "$CONFIG_FILE"
}

adb_cmd() {
  "$ADB_BIN" "$@"
}

is_authorized() {
  local serial="$1"
  [[ "$(adb_cmd -s "$serial" get-state 2>/dev/null || true)" == "device" ]]
}

add_candidate() {
  local candidate="$1"
  local existing
  if [[ -z "$candidate" ]]; then
    return 0
  fi
  for existing in "${candidates[@]}"; do
    if [[ "$existing" == "$candidate" ]]; then
      return 0
    fi
  done
  candidates+=("$candidate")
}

collect_connected_candidates() {
  local serial state
  while read -r serial state _; do
    if [[ "$state" == "device" && "$serial" == *:* ]]; then
      add_candidate "$serial"
    fi
  done < <(adb_cmd devices -l)
  return 0
}

collect_mdns_candidates() {
  local instance service address
  while read -r instance service address _; do
    if [[ "$service" == "_adb-tls-connect._tcp" ]]; then
      # Platform Tools 37+ prints: <instance> <service> <address>.
      add_candidate "$address"
    elif [[ "$instance" == *"._adb-tls-connect._tcp."* ]]; then
      # Older releases print: <instance-and-service> <address>.
      add_candidate "$service"
    fi
  done < <(adb_cmd mdns services 2>/dev/null || true)
  return 0
}

try_candidate() {
  local serial="$1"
  if is_authorized "$serial"; then
    ACTIVE_SERIAL="$serial"
    return 0
  fi

  local output
  output="$(adb_cmd connect "$serial" 2>&1 || true)"
  printf '%s\n' "$output"
  if is_authorized "$serial"; then
    ACTIVE_SERIAL="$serial"
    return 0
  fi
  return 1
}

connect_device() {
  local requested="${1:-}"
  local serial
  # Bash 3.2 with nounset treats an empty array expansion as unbound.
  candidates=("")
  ACTIVE_SERIAL=""

  adb_cmd start-server >/dev/null
  add_candidate "$requested"
  collect_connected_candidates
  add_candidate "${ADB_WIFI_SERIAL:-}"
  collect_mdns_candidates

  for serial in "${candidates[@]}"; do
    [[ -n "$serial" ]] || continue
    if try_candidate "$serial"; then
      save_serial "$ACTIVE_SERIAL"
      adb_cmd -s "$ACTIVE_SERIAL" wait-for-device
      printf 'Using wireless device %s\n' "$ACTIVE_SERIAL"
      return
    fi
  done

  printf 'No authorized wireless Android device was found.\n' >&2
  printf 'Keep Wireless debugging enabled and the Pixel on the same network.\n' >&2
  printf 'If this Mac is absent under Paired devices, run the pair command once.\n' >&2
  exit 1
}

pair_device() {
  local pair_addr="${1:-}"
  local pair_code="${2:-}"
  local connect_addr="${3:-}"
  if [[ -z "$pair_addr" || -z "$pair_code" ]]; then
    usage >&2
    exit 1
  fi

  adb_cmd start-server >/dev/null
  adb_cmd pair "$pair_addr" "$pair_code"
  connect_device "$connect_addr"
}

run_install_task() {
  local sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [[ -z "$sdk_root" ]]; then
    sdk_root="$(cd "$(dirname "$ADB_BIN")/.." && pwd)"
  fi
  (
    cd "$ROOT_DIR"
    ANDROID_HOME="$sdk_root" \
      ANDROID_SDK_ROOT="$sdk_root" \
      ANDROID_SERIAL="$ACTIVE_SERIAL" \
      ./gradlew "$INSTALL_TASK"
  )
}

install_app() {
  connect_device "${1:-}"
  run_install_task
}

launch_connected_app() {
  adb_cmd -s "$ACTIVE_SERIAL" shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 >/dev/null
  printf 'Launched %s on %s\n' "$APP_ID" "$ACTIVE_SERIAL"
}

launch_app() {
  connect_device "${1:-}"
  launch_connected_app
}

deploy_app() {
  connect_device "${1:-}"
  run_install_task
  launch_connected_app
}

main() {
  ADB_BIN="$(find_adb)"
  load_config

  local command="${1:-}"
  shift || true
  case "$command" in
    pair)
      pair_device "$@"
      ;;
    connect)
      connect_device "${1:-}"
      ;;
    install)
      install_app "${1:-}"
      ;;
    launch)
      launch_app "${1:-}"
      ;;
    deploy)
      deploy_app "${1:-}"
      ;;
    status)
      adb_cmd devices -l
      printf '\n'
      adb_cmd mdns services
      ;;
    -h|--help|help|"")
      usage
      ;;
    *)
      printf 'Unknown command: %s\n\n' "$command" >&2
      usage >&2
      exit 1
      ;;
  esac
}

main "$@"
