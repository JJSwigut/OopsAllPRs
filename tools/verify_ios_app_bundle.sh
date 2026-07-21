#!/usr/bin/env bash
set -euo pipefail

package_path="${1:-}"
required_resource="compose-resources/composeResources/oopsallprs.shared.generated.resources/files/exercises.csv"

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
    ;;
  *)
    echo "Unsupported iOS package: $package_path" >&2
    exit 2
    ;;
esac

echo "Verified iOS Compose resource: $required_resource"
