#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
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
  cat >&2 <<'EOF'
Ruby 3.1 or newer is required to run Fastlane for this repo.

Run:
  brew install ruby@3.3
  tools/bootstrap_fastlane.sh
EOF
  exit 2
fi

ruby_bin_dir="$(dirname "$selected_ruby")"
export PATH="$ruby_bin_dir:$PATH"
export FASTLANE_SKIP_UPDATE_CHECK="${FASTLANE_SKIP_UPDATE_CHECK:-1}"
export FASTLANE_HIDE_GITHUB_ISSUES="${FASTLANE_HIDE_GITHUB_ISSUES:-1}"

if ! bundle check >/dev/null 2>&1; then
  echo "Fastlane bundle is not installed. Run: tools/bootstrap_fastlane.sh" >&2
  exit 3
fi

bundle exec fastlane "$@"
