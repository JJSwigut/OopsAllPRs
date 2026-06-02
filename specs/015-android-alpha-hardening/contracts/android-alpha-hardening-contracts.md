# Contracts: Android Alpha Hardening

## Profile Local Readiness Contract

The Profile Data/Local status surface must communicate:

- Storage: data is stored locally.
- Sync: cloud sync is off/not present.
- Backup: Android Auto Backup is eligible or subject to release review.
- Rest alerts: rest timers remain safe if notification display is denied or unavailable.
- Export: local data export is available.
- Alpha gate: manual alpha validation state is known or pending.

The surface must use existing Fit design-system components and must not introduce a new debug-only destination.

## Recovery Contract

Cold-start hydration of an active workout must satisfy:

- Active workout id is preserved.
- Resume affordance remains available.
- Unexpired rest end instant remains anchored to wall clock.
- Expired rest end instant is cleared.
- Pending rest notification state is canceled after expired rest is cleared.
- Notification permission failure must not discard or corrupt the active workout.

## Empty-State Contract

Fresh-install top-level destinations must satisfy:

- Train shows an immediate way to start training and a truthful no-active-workout/no-routine state.
- History explains that completed workouts appear after finishing a workout.
- Progress explains that PRs and exercise records appear after completed logged work.
- Profile explains local ownership, export, backup/sync state, and management entry points.
- Copy must avoid suggesting that data already exists or that cloud sync is active.

## Alpha Smoke Checklist Contract

The validation artifact must include rows or sections for:

- App launch and top-level navigation.
- Start empty workout.
- Exercise search/picker, including recent/default behavior if present.
- Weighted set logging.
- Bodyweight reps-only set logging.
- Rest timer start, skip, completion, and denied-notification behavior.
- Active workout force-close/reopen recovery.
- Finish and discard workout.
- History empty and completed states.
- Progress empty and PR states.
- Profile local data/export/readiness state.
- Android build/tests and iOS shared compile gate.
