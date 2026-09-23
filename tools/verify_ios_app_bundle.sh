#!/usr/bin/env bash
set -euo pipefail

package_path="${1:-}"
required_resource="compose-resources/composeResources/oopsallprs.shared.generated.resources/files/exercises.csv"
live_activity_extension="RestTimerLiveActivityExtension.appex"

fail() {
  echo "$1" >&2
  exit 1
}

verify_live_activity_bundle() {
  local app_path="$1"
  local app_info="$app_path/Info.plist"
  local extension_path="$app_path/PlugIns/$live_activity_extension"
  local extension_info="$extension_path/Info.plist"
  local app_identifier
  local extension_identifier
  local extension_executable
  local supports_live_activities
  local extension_point

  [ -f "$app_info" ] || fail "Missing iOS app Info.plist: $app_info"
  [ -f "$extension_info" ] || fail "Missing embedded Rest Timer Live Activity extension: $extension_info"

  supports_live_activities="$(plutil -extract NSSupportsLiveActivities raw -o - "$app_info" 2>/dev/null || true)"
  [ "$supports_live_activities" = "true" ] || fail "iOS app must declare NSSupportsLiveActivities=true"

  app_identifier="$(plutil -extract CFBundleIdentifier raw -o - "$app_info")"
  extension_identifier="$(plutil -extract CFBundleIdentifier raw -o - "$extension_info")"
  [ "$extension_identifier" = "$app_identifier.RestTimerLiveActivity" ] || fail \
    "Unexpected Rest Timer Live Activity identifier: $extension_identifier"

  extension_point="$(plutil -extract NSExtension.NSExtensionPointIdentifier raw -o - "$extension_info")"
  [ "$extension_point" = "com.apple.widgetkit-extension" ] || fail \
    "Rest Timer extension must be a WidgetKit extension"

  extension_executable="$(plutil -extract CFBundleExecutable raw -o - "$extension_info")"
  [ -f "$extension_path/$extension_executable" ] || fail \
    "Rest Timer Live Activity executable is missing: $extension_path/$extension_executable"
}

if [ -z "$package_path" ]; then
  echo "Usage: tools/verify_ios_app_bundle.sh <OopsAllPRs.app|OopsAllPRs.ipa>" >&2
  exit 2
fi

case "$package_path" in
  *.app)
    if [ ! -d "$package_path" ]; then
      echo "iOS app bundle not found: $package_path" >&2
      exit 2
    fi
    if [ ! -f "$package_path/$required_resource" ]; then
      echo "Missing required iOS Compose resource: $package_path/$required_resource" >&2
      exit 1
    fi
    verify_live_activity_bundle "$package_path"
    ;;
  *.ipa)
    if [ ! -f "$package_path" ]; then
      echo "iOS IPA not found: $package_path" >&2
      exit 2
    fi
    required_entry="Payload/OopsAllPRs.app/$required_resource"
    packaged_entry="$(unzip -Z1 "$package_path" "$required_entry" 2>/dev/null || true)"
    if [ "$packaged_entry" != "$required_entry" ]; then
      echo "Missing required iOS Compose resource in IPA: $required_resource" >&2
      exit 1
    fi
    app_path="${required_entry%/$required_resource}"
    extension_info_entry="$app_path/PlugIns/$live_activity_extension/Info.plist"
    app_info_entry="$app_path/Info.plist"
    for entry in "$app_info_entry" "$extension_info_entry"; do
      packaged_entry="$(unzip -Z1 "$package_path" "$entry" 2>/dev/null || true)"
      [ "$packaged_entry" = "$entry" ] || fail "Missing required iOS Live Activity entry in IPA: $entry"
    done

    temp_dir="$(mktemp -d)"
    trap 'rm -rf "$temp_dir"' EXIT
    unzip -qq "$package_path" "$app_info_entry" "$extension_info_entry" -d "$temp_dir"
    extension_executable="$(plutil -extract CFBundleExecutable raw -o - "$temp_dir/$extension_info_entry")"
    extension_executable_entry="$app_path/PlugIns/$live_activity_extension/$extension_executable"
    packaged_entry="$(unzip -Z1 "$package_path" "$extension_executable_entry" 2>/dev/null || true)"
    [ "$packaged_entry" = "$extension_executable_entry" ] || fail \
      "Missing required iOS Live Activity executable in IPA: $extension_executable_entry"
    unzip -qq "$package_path" "$extension_executable_entry" -d "$temp_dir"
    verify_live_activity_bundle "$temp_dir/$app_path"
    ;;
  *)
    echo "Unsupported iOS package: $package_path" >&2
    exit 2
    ;;
esac

echo "Verified iOS Compose resource and Rest Timer Live Activity bundle"
