#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
project="$repo_root/iosApp/OopsAllPRs.xcodeproj"
scheme="${IOS_SMOKE_SCHEME:-OopsAllPRs}"
bundle_id="${IOS_SMOKE_BUNDLE_ID:-com.jjswigut.oopsallprs.ios.debug}"
device_type="${IOS_SMOKE_DEVICE_TYPE:-com.apple.CoreSimulator.SimDeviceType.iPhone-18-Pro}"
runtime="${IOS_SMOKE_RUNTIME:-com.apple.CoreSimulator.SimRuntime.iOS-27-0}"
artifact_dir="${IOS_SMOKE_ARTIFACT_DIR:-$repo_root/build/smoke/ios}"
derived_data_path="${IOS_SMOKE_DERIVED_DATA_PATH:-$repo_root/build/ios-derived-debug-smoke}"
app_path="$derived_data_path/Build/Products/Debug-iphonesimulator/OopsAllPRs.app"
simulator_name="OopsAllPRs smoke $(date '+%Y%m%d%H%M%S')"
created_udid=""

usage() {
  cat <<'EOF'
Usage: tools/ios_simulator_smoke.sh

Builds the debug iOS simulator app, installs it on a disposable simulator,
launches it, and captures a settled screenshot.

Environment:
  IOS_SMOKE_DEVICE_TYPE=com.apple.CoreSimulator.SimDeviceType.iPhone-18-Pro
  IOS_SMOKE_RUNTIME=com.apple.CoreSimulator.SimRuntime.iOS-27-0
  IOS_SMOKE_ARTIFACT_DIR=build/smoke/ios
  IOS_SMOKE_DERIVED_DATA_PATH=build/ios-derived-debug-smoke
  IOS_SMOKE_KEEP_SIMULATOR=1

The script creates and deletes only the simulator it created. It never selects
or modifies a physical iOS device.
EOF
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  usage
  exit 0
fi

if [ ! -d "$project" ]; then
  echo "iOS project not found: $project" >&2
  exit 2
fi

cleanup() {
  local status="$?"
  if [ -n "$created_udid" ] && [ "${IOS_SMOKE_KEEP_SIMULATOR:-0}" != "1" ]; then
    xcrun simctl shutdown "$created_udid" >/dev/null 2>&1 || true
    xcrun simctl delete "$created_udid" >/dev/null 2>&1 || true
  fi
  exit "$status"
}

trap cleanup EXIT

mkdir -p "$artifact_dir"

if ! xcrun simctl list runtimes | grep -F -q "$runtime"; then
  echo "Required iOS simulator runtime is unavailable: $runtime" >&2
  echo "Install it from Xcode Settings > Components or set IOS_SMOKE_RUNTIME." >&2
  exit 3
fi

if ! xcrun simctl list devicetypes | grep -F -q "$device_type"; then
  echo "Required simulator device type is unavailable: $device_type" >&2
  echo "Set IOS_SMOKE_DEVICE_TYPE to an installed device type." >&2
  exit 3
fi

cd "$repo_root"

created_udid="$(xcrun simctl create "$simulator_name" "$device_type" "$runtime")"
if [ -z "$created_udid" ]; then
  echo "Could not create disposable iOS simulator." >&2
  exit 5
fi

xcodebuild build -quiet \
  -project "$project" \
  -scheme "$scheme" \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination "id=$created_udid" \
  -derivedDataPath "$derived_data_path" \
  CODE_SIGNING_ALLOWED=NO

if [ ! -d "$app_path" ]; then
  echo "Expected simulator app was not built: $app_path" >&2
  exit 4
fi

xcrun simctl boot "$created_udid"
xcrun simctl bootstatus "$created_udid" -b
xcrun simctl install "$created_udid" "$app_path"

launch_output="$(xcrun simctl launch "$created_udid" "$bundle_id")"
sleep "${IOS_SMOKE_LAUNCH_SETTLE_SECONDS:-15}"

screenshot_path="$artifact_dir/launch.png"
launch_path="$artifact_dir/launch.txt"
xcrun simctl io "$created_udid" screenshot "$screenshot_path"
printf '%s\n' "$launch_output" > "$launch_path"

if [ ! -s "$screenshot_path" ]; then
  echo "iOS app launched but no screenshot was captured: $screenshot_path" >&2
  exit 6
fi

# A launch command and PNG alone can capture the simulator's blank startup
# surface. Sample the rendered image to reject that case without assuming a
# particular screen or locale.
if ! swift - "$screenshot_path" <<'SWIFT'
import AppKit
import Foundation

let path = CommandLine.arguments[1]
guard let image = NSImage(contentsOfFile: path),
      let tiff = image.tiffRepresentation,
      let bitmap = NSBitmapImageRep(data: tiff) else {
    exit(1)
}

var colors = Set<UInt32>()
let xStride = max(1, bitmap.pixelsWide / 32)
let yStride = max(1, bitmap.pixelsHigh / 64)
for y in stride(from: 0, to: bitmap.pixelsHigh, by: yStride) {
    for x in stride(from: 0, to: bitmap.pixelsWide, by: xStride) {
        guard let color = bitmap.colorAt(x: x, y: y)?.usingColorSpace(.deviceRGB) else { continue }
        let red = UInt32((color.redComponent * 255).rounded())
        let green = UInt32((color.greenComponent * 255).rounded())
        let blue = UInt32((color.blueComponent * 255).rounded())
        colors.insert((red << 16) | (green << 8) | blue)
    }
}

exit(colors.count >= 16 ? 0 : 1)
SWIFT
then
  echo "iOS app did not render a sufficiently varied UI after launch. Screenshot: $screenshot_path" >&2
  exit 6
fi

cat <<EOF
iOS simulator smoke passed.
Simulator: $created_udid ($simulator_name)
Runtime: $runtime
Device type: $device_type
Bundle: $bundle_id
App: $app_path
Screenshot: $screenshot_path
Launch output: $launch_path
EOF
