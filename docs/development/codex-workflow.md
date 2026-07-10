# Codex Product Workflow

This repo uses `development` as the integration branch and `main` as the release branch. The default Codex operating model is one parent/orchestrator thread plus short-lived worker threads.

## Parent Orchestrator

Use `$thread-orchestrator` for product-level requests such as "add this feature", "check the workers", "prepare a release", or "cut a release".

The parent thread owns:

- Product conversation, scoping, sequencing, and decisions.
- The project ledger at `/Users/swig/thread-orchestrator/oops-all-prs.md`.
- Worker thread creation, naming, monitoring, and archiving.
- Release promotion requests from `development` to `main`.

The parent thread does not make public mutations by default. Pushing branches, opening pull requests, merging, creating GitHub releases, using credentials, deploying, or publishing requires explicit owner approval in the current conversation.

## Feature Flow

When the owner says "add this feature":

1. Read `AGENTS.md`, this workflow, and the active feature/spec context.
2. Run the base-ref preflight before creating a worker:

   ```bash
   git fetch --prune origin
   git rev-list --left-right --count development...origin/development
   ```

   The two counts are `<local-ahead> <remote-ahead>`. Treat them as product-routing information, not as permission to reset or rewrite either ref.

3. Choose the worker base ref:

   - If the owner asks for "latest develop/development", use fetched `origin/development`.
   - If the owner asks for local integration work or the parent is carrying unpushed integration commits, use local `development`.
   - If local `development` and `origin/development` have both moved, stop and ask which base to use.
   - Never force-reset or rewrite local `development` as part of worker creation.

4. Create a project-scoped Codex worker thread from the chosen base ref with a worktree environment using:

   - Project id: `/Users/swig/Development/oops-all-prs`
   - Environment: `worktree`
   - Starting state: the chosen base ref, usually `development` or `origin/development`
   - Prompt: the Worker Contract below, filled with the feature scope

   This is the preferred path because the Codex app owns the worker thread and its worktree. A Codex-created worktree may report `HEAD (no branch)` even when `startingState` names `development`; verify the starting state by comparing `git rev-parse HEAD` to the intended local ref.

   Codex thread target fields:

   ```text
   type: project
   projectId: /Users/swig/Development/oops-all-prs
   environment: worktree
   startingState: branch development
   ```

   To start from the latest fetched remote development ref, fetch `origin` first and use:

   ```text
   type: project
   projectId: /Users/swig/Development/oops-all-prs
   environment: worktree
   startingState: branch origin/development
   ```

   Do this only when the owner asks for latest remote development. Otherwise use local `development` so unpushed local integration work is not silently bypassed.

5. If a manual local worktree is needed instead, create a short-lived feature branch and worktree with:

   ```bash
   tools/create_feature_worktree.sh feature-name
   ```

   To branch from the latest fetched remote development branch:

   ```bash
   FROM_ORIGIN=1 tools/create_feature_worktree.sh feature-name
   ```

6. Assign one bounded lane to the worker. The assignment must say `Use $thread-worker`.
7. Require focused automated proof, Android emulator proof when UI/runtime behavior is touched, and a clean final state report.
8. After owner approval, push the feature branch and open a normal PR against `development`.

Default worktree root:

```text
/Users/swig/Development/oops-all-prs-worktrees
```

## Worker Contract

Every implementation worker receives:

- Repo path and worktree path.
- Branch name/worktree identity and base branch.
- Scope and explicit out-of-scope items.
- Authorized actions: local edits and local verification only unless the parent grants more.
- Required proof: focused tests, build checks, and emulator/device smoke proof for runtime UI behavior.
- Stop conditions: product ambiguity, missing credential/access, public mutation, destructive action, release action, cross-thread conflict, or inability to verify honestly.
- No-subdelegation rule.

Prompt shape:

```text
Use $thread-worker.

Parent objective: Improve Oops All PRs through a controlled feature workflow.
Your lane: Implement <feature> in the Codex-created worktree based on development.

Scope:
- In scope: <specific files/flows/specs>.
- Out of scope: merging, releases, store publishing, unrelated refactors.

Permissions:
- Authorized: local edits, local tests, Android emulator/device smoke checks.
- Not authorized: push, PR creation, PR comments, merge, deploy, release, credential use, destructive git operations.

Success criteria:
- Feature behavior works as specified.
- Relevant automated tests pass.
- Starting commit matches the assigned base ref, even if the worker checkout is detached.
- Android runtime flow is launched and smoke-checked when app behavior changed.
- Workspace status is reported clearly.

Required proof:
- Commands run and results.
- Base ref and `HEAD` commit comparison.
- Emulator/device id used, or exact blocker if no device is available.
- Screenshots/logs when a UI smoke check is meaningful.

Stop and report if:
- Product behavior is ambiguous.
- Credentials, store accounts, protected environments, destructive operations, or public mutations are required.
- Verification cannot be performed honestly.
- Work conflicts with user or other-thread changes.

Do not create subworkers, manage other threads, or edit the parent ledger.
Report back using the $thread-worker completion format.
```

Worker completion format:

```text
Result: completed | blocked | needs owner | partial
Scope: <assigned lane and what was actually touched>
Changes: <files/artifacts/URLs or "none">
Proof: <commands, tests, live checks, review, citations, screenshots>
State: <branch/worktree/thread state, dirty files, pushed/unpushed status>
Decision needed: <exact ask, recommendation, and consequences, or "none">
Next: <one concrete next step for the parent>
```

Worker result semantics:

- `completed`: the lane is done, required proof is complete, and no required worker work remains.
- `blocked`: the worker cannot continue without a specific owner action, permission, credential, external state change, or unavailable verification environment. `Decision needed` must name the exact unblocker; it cannot be `none`.
- `needs owner`: implementation can proceed only after a product or scope choice by the owner. Include the recommended choice and consequences.
- `partial`: a coherent subset is implemented or researched with proof, but the full lane intentionally remains incomplete; include remaining work.

Setup-only work such as fetching refs, creating a branch, switching worktrees, installing dependencies, reading instructions, or outlining an approach is a checkpoint, not completion. If a worker reports `blocked` but no owner action is needed, the parent should treat it as an invalid setup checkpoint and resume the worker with the original assignment.

## Local Verification

Use the smallest credible verification for a feature, then broaden when shared behavior changes.

Baseline checks:

```bash
./gradlew --no-daemon check
./gradlew --no-daemon :androidApp:assembleDebug
```

Android runtime smoke:

```bash
tools/android_emulator_smoke.sh
```

If no Android device is online, the script starts the first configured AVD or the AVD named by `ANDROID_AVD_NAME`.

Release gate:

```bash
tools/release_gate.sh
```

`tools/release_gate.sh --android-smoke` also installs and launches the debug Android app on an attached device or running emulator.

Successful release gates write proof files under `build/release-gate/`, including `summary.txt`, `artifacts.txt`, and `proof.env`.

iOS Release simulator package:

```bash
tools/ios_release_package.sh
```

Fastlane/store-lane bootstrap:

```bash
tools/bootstrap_fastlane.sh
tools/fastlane.sh lanes
```

Local Fastlane checks require Ruby 3.1 or newer. The macOS system Ruby 2.6 is not sufficient.

## Pull Requests

Normal feature PRs target `development`.

The worker may prepare local commits only when the parent explicitly grants commit permission. Push and PR creation require explicit owner approval unless the owner grants those permissions for the lane.

Use `.github/pull_request_template.md` for normal feature PRs and deployment/release PRs.

Expected PR proof:

- Summary of user-visible behavior.
- Automated tests and builds run.
- Emulator/device smoke result or documented blocker.
- Known risks and follow-ups.

## Release Flow

When the owner says "cut a release":

1. Confirm release intent and approval to promote `development` into `main`.
2. Ensure local `development` includes the intended changes.
3. Run:

   ```bash
   tools/release_gate.sh --android-smoke
   ```

4. If gates pass, open a deployment/release PR from `development` to `main` unless the owner explicitly asks for a direct merge.
5. After owner approval and merge to `main`, GitHub Actions runs `.github/workflows/deploy.yml`.
6. The deploy workflow runs `tools/release_gate.sh --skip-ios`, uploads Android release artifacts and `build/release-gate/` proof files, attempts `tools/ios_release_package.sh` as a timeout-bounded iOS Release simulator package, uploads GitHub Actions artifacts, and creates a GitHub Release on `main` pushes with an automatic `v1.0.<run-number + 1000>` tag. Manual dispatch can override the release tag.
7. Google Play and App Store upload jobs remain credential-gated. They skip with notices until the required secrets are configured.

Current iOS note: local Release simulator packaging can take longer than debug framework linking and may stall in `:shared:linkReleaseFrameworkIosSimulatorArm64`. Until that is fixed, Android release artifacts and any successful iOS package artifacts are published to GitHub, but iOS packaging is best-effort and timeout-bounded so it does not block the Android/GitHub release package.

## GitHub Actions

- `.github/workflows/ci.yml`: pull requests plus pushes to `development` and `main`.
- `.github/workflows/deploy.yml`: pushes to `main` and manual dispatch. It runs `tools/release_gate.sh --skip-ios`, uploads release packages and proof files to GitHub Actions artifacts, creates a GitHub Release for `main` pushes or manual dispatch with a tag, runs iOS simulator packaging as best-effort, and stubs store publishing when secrets are missing.
