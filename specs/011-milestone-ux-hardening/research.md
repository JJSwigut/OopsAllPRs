# Research: Milestone Validation and UX Hardening

## Decision: Use a milestone evidence ledger for manual validation

**Rationale**: Previous features deferred manual gates because a device/emulator
was not available. A single milestone evidence ledger can record the device,
build, screenshots, findings, and fixes once, then reference it from earlier
validation files.

**Alternatives considered**:

- Duplicate full manual notes in every older feature: rejected because it
  creates drift and makes future evidence harder to audit.
- Keep the gates deferred: rejected because an emulator is now available and the
  current product risk is UX confidence, not more feature scope.

## Decision: Treat emulator System UI ANRs separately from app defects

**Rationale**: The fresh Android emulator image has shown a System UI warning
while the app remains focused and responsive behind it. That should be recorded
as environment noise unless it prevents app operation.

**Alternatives considered**:

- Fail the app milestone on any platform dialog: rejected because it would
  conflate emulator image instability with app behavior.
- Ignore the warning entirely: rejected because future reviewers need to know
  why screenshots may include or mention it.

## Decision: Fix only reproducible UX hardening defects

**Rationale**: This slice is release-readiness polish. It should improve thumb
reach, overlap, keyboard behavior, accessibility, and interaction feedback where
manual review proves a defect, but it should not redesign the product or add new
flows.

**Alternatives considered**:

- Start a new UX redesign: rejected because specs 006-010 already established a
  usable loop and the current need is confidence and polish.
- Only document defects: rejected because several expected findings are likely
  small shared UI fixes that should be handled immediately.

## Decision: Keep validation Android-first while preserving shared/iOS gates

**Rationale**: Android provides the runtime target for manual observation, while
shared Compose/state changes can still affect iOS. The milestone should run both
Android and iOS compile gates after any shared changes.

**Alternatives considered**:

- Android-only validation: rejected because it risks shared UI regressions on
  the iOS target.
- Native iOS manual validation now: rejected because Android-first remains the
  current implementation sequence and iOS runtime shell polish is not in scope.

## Decision: Verify interaction accessibility through state and manual review

**Rationale**: The current project primarily has state/model tests, not
instrumented Compose semantics tests. This slice should add focused tests where
state behavior changes and use manual device review for TalkBack labels, touch
targets, motion, and visual feedback until instrumented UI test infrastructure
is introduced.

**Alternatives considered**:

- Add full instrumented UI tests in this slice: rejected because it broadens the
  infrastructure scope and was already deferred in the design-system plan.
- Skip accessibility review: rejected by the constitution and public release
  gate requirements.
