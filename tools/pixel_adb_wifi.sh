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
  - The last successful connect-ip:port is saved in .env.pixel-adb, which is ignored by .gitignore.

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

require_serial() {
  local serial="${1:-${ADB_WIFI_SERIAL:-}}"
  if [[ -z "$serial" ]]; then
    printf 'Missing connect-ip:port. Run pair with a connect address or run connect <ip:port> once.\n\n' >&2
    usage >&2
    exit 1
  fi
  printf '%s\n' "$serial"
}

adb_cmd() {
  "$ADB_BIN" "$@"
}

connect_device() {
  local serial
  serial="$(require_serial "${1:-}")"
  adb_cmd start-server >/dev/null
  local output
  output="$(adb_cmd connect "$serial" 2>&1 || true)"
  printf '%s\n' "$output"
  if [[ "$output" != *"connected to"* && "$output" != *"already connected"* ]]; then
    printf 'Could not connect to %s.\n' "$serial" >&2
    exit 1
  fi
  save_serial "$serial"
  adb_cmd -s "$serial" wait-for-device
  printf 'Using wireless device %s\n' "$serial"
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
  if [[ -n "$connect_addr" ]]; then
    connect_device "$connect_addr"
  else
    printf '\nPaired. Now run:\n'
    printf '  tools/pixel_adb_wifi.sh connect <connect-ip:port>\n'
  fi
}

install_app() {
  local serial
  serial="$(require_serial "${1:-}")"
  connect_device "$serial"
  (cd "$ROOT_DIR" && ANDROID_SERIAL="$serial" ./gradlew "$INSTALL_TASK")
}

launch_app() {
  local serial
  serial="$(require_serial "${1:-}")"
  connect_device "$serial"
  adb_cmd -s "$serial" shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 >/dev/null
  printf 'Launched %s on %s\n' "$APP_ID" "$serial"
}

deploy_app() {
  local serial
  serial="$(require_serial "${1:-}")"
  install_app "$serial"
  launch_app "$serial"
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
