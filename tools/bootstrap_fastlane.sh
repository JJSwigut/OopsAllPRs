#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"

usage() {
  cat <<'EOF'
Usage: tools/bootstrap_fastlane.sh

Installs Fastlane gems for this repo into vendor/bundle.

Requirements:
  - Ruby 3.1 or newer on PATH, or Homebrew ruby@3.3/ruby@3.4 installed
  - Bundler available for that Ruby

This script does not install Ruby itself. On macOS, install a current Ruby with
Homebrew, rbenv, mise, asdf, or another Ruby manager, then rerun this script.
EOF
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  usage
  exit 0
fi

cd "$repo_root"

ruby_candidates=()
if command -v ruby >/dev/null 2>&1; then
  ruby_candidates+=("$(command -v ruby)")
fi
ruby_candidates+=(
  "/opt/homebrew/opt/ruby@3.3/bin/ruby"
  "/opt/homebrew/opt/ruby@3.4/bin/ruby"
  "/opt/homebrew/opt/ruby/bin/ruby"
)

selected_ruby=""
for candidate in "${ruby_candidates[@]}"; do
  if [ -x "$candidate" ]; then
    if "$candidate" -e 'major, minor, = RUBY_VERSION.split(".").map(&:to_i); exit(major > 3 || (major == 3 && minor >= 1) ? 0 : 1)' >/dev/null 2>&1; then
      selected_ruby="$candidate"
      break
    fi
  fi
done

if [ -z "$selected_ruby" ]; then
  echo "Ruby 3.1 or newer is not available." >&2
  if command -v ruby >/dev/null 2>&1; then
    echo "Current ruby: $(command -v ruby) ($(ruby -v))" >&2
  fi
  usage >&2
  exit 2
fi

ruby_bin_dir="$(dirname "$selected_ruby")"
export PATH="$ruby_bin_dir:$PATH"

ruby_version="$(ruby -e 'print RUBY_VERSION')"
ruby_ok="$(ruby -e 'major, minor, = RUBY_VERSION.split(".").map(&:to_i); print((major > 3 || (major == 3 && minor >= 1)) ? "yes" : "no")')"

if [ "$ruby_ok" != "yes" ]; then
  cat >&2 <<EOF
Ruby $ruby_version is too old for this repo's Fastlane environment.

Use Ruby 3.1 or newer, then rerun:
  tools/bootstrap_fastlane.sh

Current ruby:
  $(command -v ruby)
EOF
  exit 3
fi

if ! command -v bundle >/dev/null 2>&1; then
  echo "Bundler is not available for Ruby $ruby_version. Install it with: gem install bundler" >&2
  exit 4
fi

bundle config set path vendor/bundle
bundle config set clean true
bundle install

cat <<EOF
Fastlane bundle is ready.
Ruby: $(ruby -v)
Bundler: $(bundle -v)

Useful checks:
  tools/fastlane.sh lanes
  tools/fastlane.sh android listing
  tools/fastlane.sh ios listing
EOF
