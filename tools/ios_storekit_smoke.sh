#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
project="$repo_root/iosApp/OopsAllPRs.xcodeproj"
scheme="${IOS_STOREKIT_SCHEME:-OopsAllPRs}"
destination="${IOS_STOREKIT_DESTINATION:-platform=iOS Simulator,name=${IOS_STOREKIT_SIMULATOR_NAME:-iPhone 18 Pro},OS=${IOS_STOREKIT_RUNTIME:-27.0}}"
result_dir="${IOS_STOREKIT_RESULT_DIR:-$repo_root/build/ios-storekit}"
result_bundle="$result_dir/test-$(date '+%Y%m%d%H%M%S').xcresult"

if [ ! -d "$project" ]; then
  echo "iOS project not found: $project" >&2
  exit 2
fi

mkdir -p "$result_dir"
"$repo_root/tools/verify_ios_storekit_fixture.sh"

xcodebuild test -quiet \
  -project "$project" \
  -scheme "$scheme" \
  -destination "$destination" \
  -only-testing:OopsAllPRsStoreKitTests \
  -resultBundlePath "$result_bundle"

summary="$(xcrun xcresulttool get test-results summary --path "$result_bundle")"
if ! printf '%s\n' "$summary" | grep -q '"result" : "Passed"'; then
  printf '%s\n' "$summary" >&2
  echo "StoreKit tests did not pass." >&2
  exit 1
fi

printf '%s\n' "$summary" | grep -E '"(result|passedTests|failedTests|totalTestCount)"'
echo "StoreKit result: $result_bundle"
