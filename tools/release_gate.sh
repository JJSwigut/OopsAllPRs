#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
run_android_smoke=0
skip_ios=0

usage() {
  cat <<'EOF'
Usage: tools/release_gate.sh [--android-smoke] [--skip-ios]

Runs the local release gate before promoting development to main.

Checks:
  - Gradle shared/design/system checks
  - Android debug and release assembly
  - Android release bundle packaging
  - iOS simulator framework compile, unless --skip-ios
  - Optional Android emulator/device launch smoke
EOF
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --android-smoke) run_android_smoke=1 ;;
    --skip-ios) skip_ios=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage >&2; exit 1 ;;
  esac
  shift
done

cd "$repo_root"
release_gate_dir="build/release-gate"
summary_path="$release_gate_dir/summary.txt"
artifacts_path="$release_gate_dir/artifacts.txt"
proof_path="$release_gate_dir/proof.env"
android_smoke_log="$release_gate_dir/android-smoke.txt"
started_at="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
ios_status="skipped"
android_smoke_status="not-run"

mkdir -p "$release_gate_dir"

echo "Release gate started: $started_at" | tee "$summary_path"
git status --short --branch | tee -a "$summary_path"

./gradlew --no-daemon \
  check \
  :androidApp:assembleDebug \
  :androidApp:assembleRelease \
  :androidApp:bundleRelease

if [ "$skip_ios" -eq 0 ]; then
  ./gradlew --no-daemon :shared:linkDebugFrameworkIosSimulatorArm64
  ios_status="passed"
else
  echo "Skipping iOS simulator framework compile by request." | tee -a "$summary_path"
fi

if [ "$run_android_smoke" -eq 1 ]; then
  tools/android_emulator_smoke.sh | tee "$android_smoke_log"
  android_smoke_status="passed"
fi

find androidApp/build/outputs -type f \( -name '*.apk' -o -name '*.aab' -o -name 'mapping.txt' -o -name 'resources.txt' \) | sort > "$artifacts_path"
completed_at="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"

{
  echo
  echo "Artifacts:"
  cat "$artifacts_path"
  echo
  echo "Release gate completed: $completed_at"
} | tee -a "$summary_path"

{
  echo "started_at=$started_at"
  echo "completed_at=$completed_at"
  echo "git_branch=$(git branch --show-current)"
  echo "git_commit=$(git rev-parse HEAD)"
  echo "ios_debug_framework=$ios_status"
  echo "android_smoke=$android_smoke_status"
  echo "summary=$summary_path"
  echo "artifacts=$artifacts_path"
  if [ "$run_android_smoke" -eq 1 ]; then
    echo "android_smoke_log=$android_smoke_log"
  fi
} > "$proof_path"

echo "Proof: $proof_path" | tee -a "$summary_path"
