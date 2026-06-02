# Research: Android Alpha Hardening

## Decision: Treat Alpha Readiness As A Release Gate, Not A Feature Screen

**Rationale**: Dogfood confidence needs repeatable evidence, but a dedicated in-app alpha/debug screen would add product surface that users should not see. Profile can expose user-facing local ownership and readiness facts, while the detailed smoke checklist lives in feature validation docs.

**Alternatives considered**:

- Add a hidden debug alpha screen: rejected because it creates navigation and release-surface debt.
- Keep readiness only in docs: rejected because users need in-app clarity about local data, export, and sync status.

## Decision: Keep Profile Readiness Static For This Slice

**Rationale**: Current requirements are clarity-focused. Dynamic notification permission and backup introspection would require platform-specific APIs and states that are not needed to prove denied-permission safety. Static wording can accurately describe expected behavior and keep cloud/sync claims honest.

**Alternatives considered**:

- Platform adapter for live notification permission status: deferred until notification prompting/settings UX is designed.
- Runtime backup eligibility detector: rejected because Android backup policy is mostly manifest/config driven and should be release-reviewed separately.

## Decision: Validate Expired Rest Hydration In Shared Use-Case Tests

**Rationale**: Rest timers use shared wall-clock anchors and notification scheduling is already abstracted. A shared test can prove the active workout remains resumable, expired rest is cleared, and scheduler cancellation is requested without relying on device timing.

**Alternatives considered**:

- Manual-only rest recovery testing: rejected because process-death timing is easy to miss manually.
- Android instrumentation-only test: deferred; shared coverage is faster and protects iOS semantics too.

## Decision: Improve Empty States With Copy And Existing Components Only

**Rationale**: The UX has already had layout iteration. This hardening slice should make fresh-install screens clearer without reopening navigation or component design. Existing Fit cards, text, and buttons are enough.

**Alternatives considered**:

- Redesign top-level screens: rejected for scope.
- Add illustrative art: rejected because the app should stay dense, gym-practical, and design-system driven.

## Decision: Manual Pixel Validation Can Be Deferred With Evidence

**Rationale**: The user may run the app on a physical Pixel, but the implementation should not block on device availability during coding. Validation docs must explicitly record whether a physical device, emulator, or deferred manual path was used.

**Alternatives considered**:

- Require physical device before merge: too brittle for local development.
- Skip manual validation notes entirely: rejected because public-release quality gates require recorded milestone evidence.
