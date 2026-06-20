<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read
`specs/024-cloud-backup-sync/plan.md`.
<!-- SPECKIT END -->

## Git Working Conventions

- `main` is the deploy/release branch. Keep it stable and only update it when intentionally promoting work for deployment.
- `development` is the default integration branch for active work. Start new implementation, fixes, cleanup, and documentation changes from `development`, not `main`.
- Use short-lived feature branches for isolated work when the change is larger than a small direct update to `development`. Branch from `development` and merge back into `development` after review and verification.
- Open normal pull requests against `development`. Open deployment/release pull requests from `development` into `main` only when the app is ready to ship.
- Before merging `development` into `main`, run the relevant verification from the feature plan, including Gradle unit tests for shared code and any Android/iOS smoke checks touched by the change.
- If you discover you are on `main` before starting work, switch to `development` first. If uncommitted work already exists on `main`, create or switch to `development` at the same commit and carry the working tree there; do not reset or discard work.
- Do not commit directly to `main` except for urgent release metadata or deployment hotfixes. Hotfixes made on `main` must be merged or cherry-picked back into `development` immediately after release.
- Keep `development` regularly updated with `main` after releases so active work includes deployed hotfixes and release metadata.
- This repo uses `.githooks/pre-commit` via `core.hooksPath=.githooks` to block accidental direct commits on `main`. For an intentional release metadata commit or urgent hotfix, use `ALLOW_MAIN_COMMIT=1 git commit` and then bring the change back to `development`.
