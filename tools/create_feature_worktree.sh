#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: tools/create_feature_worktree.sh <feature-name-or-branch> [worktree-root]

Creates a short-lived feature branch and worktree from local development.

Environment:
  BASE_BRANCH=development
  BRANCH_PREFIX=swiggy/
  FETCH_REMOTE=0
  FROM_ORIGIN=0

Examples:
  tools/create_feature_worktree.sh rest-timer-polish
  FROM_ORIGIN=1 tools/create_feature_worktree.sh backup-ui-copy
  BASE_BRANCH=origin/development tools/create_feature_worktree.sh backup-ui-copy
EOF
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ] || [ "$#" -lt 1 ]; then
  usage
  exit 0
fi

repo_root="$(git rev-parse --show-toplevel)"
base_branch_was_set=0
if [ "${BASE_BRANCH+x}" = "x" ]; then
  base_branch_was_set=1
fi

base_branch="${BASE_BRANCH:-development}"
branch_prefix="${BRANCH_PREFIX:-swiggy/}"
raw_name="$1"
worktree_root="${2:-/Users/swig/Development/oops-all-prs-worktrees}"

if [ "${FROM_ORIGIN:-0}" = "1" ]; then
  FETCH_REMOTE=1
  if [ "$base_branch_was_set" -eq 0 ]; then
    base_branch="origin/development"
  fi
fi

slug="$(
  printf '%s' "$raw_name" \
    | tr '[:upper:]' '[:lower:]' \
    | sed -E 's#[^a-z0-9._/-]+#-#g; s#^-+##; s#-+$##; s#/+#/#g'
)"

if [ -z "$slug" ]; then
  echo "Could not derive a branch slug from '$raw_name'." >&2
  exit 1
fi

case "$slug" in
  "$branch_prefix"*) branch="$slug" ;;
  */*) branch="$slug" ;;
  *) branch="${branch_prefix}${slug}" ;;
esac

worktree_name="$(printf '%s' "$branch" | tr '/' '-')"
worktree_path="$worktree_root/$worktree_name"

cd "$repo_root"

if [ "${FETCH_REMOTE:-0}" = "1" ]; then
  git fetch --prune origin
fi

git rev-parse --verify --quiet "$base_branch" >/dev/null || {
  echo "Base branch/ref '$base_branch' does not exist." >&2
  exit 1
}

if git show-ref --verify --quiet "refs/heads/$branch"; then
  echo "Branch '$branch' already exists." >&2
  exit 1
fi

if [ -e "$worktree_path" ]; then
  echo "Worktree path already exists: $worktree_path" >&2
  exit 1
fi

mkdir -p "$worktree_root"
git worktree add -b "$branch" "$worktree_path" "$base_branch"

(
  cd "$worktree_path"
  git config core.hooksPath .githooks
)

cat <<EOF
Created feature worktree.
Branch: $branch
Base: $base_branch
Path: $worktree_path

Worker prompt seed:
Use \$thread-worker.

Parent objective: Improve Oops All PRs through the project feature workflow.
Your lane: Implement <feature scope> in $worktree_path on branch $branch, based on $base_branch.

Authorized: local edits, focused tests, local builds, Android emulator/device smoke checks.
Not authorized: push, PR creation, merge, deploy, release, credential use, destructive git operations.
Required proof: commands run, results, base ref and HEAD commit comparison, emulator/device id or blocker, dirty worktree status.
Stop and report for product ambiguity, missing access, public mutation, destructive action, cross-thread conflict, or unverifiable behavior.
Do not create subworkers, manage other threads, or edit the parent ledger.
Report format:
Result: completed | blocked | needs owner | partial
Scope: <assigned lane and what was actually touched>
Changes: <files/artifacts/URLs or "none">
Proof: <commands, tests, live checks, screenshots>
State: <branch/worktree/thread state, dirty files, pushed/unpushed status>
Decision needed: <exact ask, recommendation, and consequences, or "none">
Next: <one concrete next step for the parent>

Setup-only work such as fetching refs, creating this branch/worktree, reading instructions, or installing dependencies is a checkpoint, not completion or blockage. Continue the lane after setup unless a real stop condition applies.
EOF
