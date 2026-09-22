#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
fixture_path="${IOS_STOREKIT_FIXTURE:-$repo_root/iosApp/iosApp/FullAccess.storekit}"
config_path="$repo_root/shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/FullAccessBillingConfig.kt"

if [ ! -f "$fixture_path" ]; then
  echo "StoreKit fixture not found: $fixture_path" >&2
  exit 2
fi

product_id="$(sed -nE 's/.*LIFETIME_UNLOCK_PRODUCT_ID: String = "([^"]+)".*/\1/p' "$config_path")"
if [ -z "$product_id" ]; then
  echo "Could not read the shared lifetime product ID from $config_path" >&2
  exit 3
fi

ruby -rjson -e '
  fixture_path, expected_id = ARGV
  fixture = JSON.parse(File.read(fixture_path))
  products = fixture.fetch("products")
  abort("Expected exactly one StoreKit product") unless products.length == 1
  product = products.first
  abort("StoreKit product ID does not match shared billing config") unless product.fetch("productID") == expected_id
  abort("StoreKit product must be non-consumable") unless product.fetch("type") == "NonConsumable"
  abort("StoreKit fixture price must be 14.99") unless product.fetch("displayPrice") == "14.99"
  localization = product.fetch("localizations").find { |value| value["locale"] == "en_US" }
  abort("StoreKit fixture needs an English localization") unless localization
  abort("StoreKit fixture needs a display name") if localization.fetch("displayName").strip.empty?
  puts "Verified local StoreKit fixture: #{product.fetch("productID")} (#{product.fetch("type")}, $#{product.fetch("displayPrice")})"
' "$fixture_path" "$product_id"
