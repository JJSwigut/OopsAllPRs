#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
max_seconds="${IOS_RELEASE_TIMEOUT_SECONDS:-900}"
derived_data_path="${IOS_DERIVED_DATA_PATH:-$repo_root/build/ios-derived-release}"
artifact_dir="${IOS_RELEASE_ARTIFACT_DIR:-$repo_root/build/release-artifacts}"
product_dir="$derived_data_path/Build/Products/Release-iphonesimulator"

usage() {
  cat <<'EOF'
Usage: tools/ios_release_package.sh

Builds and packages the iOS Release simulator app.

Environment:
  IOS_RELEASE_TIMEOUT_SECONDS=900
  IOS_DERIVED_DATA_PATH=build/ios-derived-release
  IOS_RELEASE_ARTIFACT_DIR=build/release-artifacts

This is a release packaging aid, not App Store upload. It is safe to run
without Apple signing secrets because CODE_SIGNING_ALLOWED=NO is used.
EOF
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  usage
  exit 0
fi

cd "$repo_root"

if ! command -v xcodebuild >/dev/null 2>&1; then
  echo "xcodebuild is not available on PATH." >&2
  exit 2
fi

mkdir -p "$artifact_dir"
rm -rf "$derived_data_path" "$artifact_dir"/OopsAllPRs-release-simulator-*.zip

run_with_timeout() {
  local timeout_seconds="$1"
  shift
  "$@" &
  local pid="$!"
  local start="$SECONDS"

  terminate_tree() {
    local root_pid="$1"
    local signal="$2"
    local child
    for child in $(pgrep -P "$root_pid" 2>/dev/null || true); do
      terminate_tree "$child" "$signal"
    done
    kill "-$signal" "$root_pid" 2>/dev/null || true
  }

  while kill -0 "$pid" 2>/dev/null; do
    if [ $((SECONDS - start)) -ge "$timeout_seconds" ]; then
      echo "Timed out after ${timeout_seconds}s: $*" >&2
      terminate_tree "$pid" TERM
      sleep 5
      terminate_tree "$pid" KILL
      wait "$pid" 2>/dev/null || true
      return 124
    fi
    sleep 5
  done

  wait "$pid"
}

run_with_timeout "$max_seconds" \
  xcodebuild \
    -quiet \
    -project iosApp/OopsAllPRs.xcodeproj \
    -scheme OopsAllPRs \
    -configuration Release \
    -sdk iphonesimulator \
    -destination "generic/platform=iOS Simulator" \
    -derivedDataPath "$derived_data_path" \
    ARCHS=arm64 \
    ONLY_ACTIVE_ARCH=YES \
    CODE_SIGNING_ALLOWED=NO \
    build

app_path="$product_dir/OopsAllPRs.app"
if [ ! -d "$app_path" ]; then
  echo "Expected iOS app not found: $app_path" >&2
  exit 3
fi

ditto -c -k --keepParent "$app_path" "$artifact_dir/OopsAllPRs-release-simulator-app.zip"

dsym_path="$product_dir/OopsAllPRs.app.dSYM"
if [ -d "$dsym_path" ]; then
  ditto -c -k --keepParent "$dsym_path" "$artifact_dir/OopsAllPRs-release-simulator-dSYM.zip"
fi

cat <<EOF
iOS Release simulator package created.
App: $app_path
Artifacts:
$(find "$artifact_dir" -maxdepth 1 -type f -name 'OopsAllPRs-release-simulator-*.zip' | sort)
EOF
