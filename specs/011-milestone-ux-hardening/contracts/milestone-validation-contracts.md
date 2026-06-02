# Contracts: Milestone Validation and UX Hardening

## Manual Evidence Contract

Every completed manual scenario must be recorded in
`specs/011-milestone-ux-hardening/validation/milestone-results.md` with:

- Scenario id and title.
- Target device/emulator.
- Result: `PASS`, `FAIL`, `PARTIAL`, or `BLOCKED`.
- Steps performed.
- Evidence paths or written notes.
- Findings created or closed.

## Core Flow Contract

The milestone core flow is considered valid only when all of these are true:

- A workout can be started or resumed from Train.
- A seeded weighted exercise can be found without relying exclusively on search.
- A bodyweight exercise can be selected and logged with reps only.
- A weighted set can be corrected with direct numeric entry before logging.
- The finish action is discoverable from the active workout surface.
- The completed workout can be found from History.
- No primary control overlaps system bars, keyboard, bottom navigation, or
  neighboring controls during the flow.

## Cross-Destination Contract

History, template, Progress, and Profile/export continuity is considered valid
only when:

- Completed workout details are readable in History.
- Template save or launch flow is either completed or explicitly blocked by a
  valid active-workout conflict state.
- Progress shows PR data or a truthful empty state using local data.
- Profile export reports success or a recoverable platform limitation.
- None of these flows mutate completed sets outside normal user actions.

## Accessibility and Interaction Contract

The milestone polish pass must verify:

- Primary controls meet the app touch target expectation.
- Labels and state descriptions are meaningful for top-level destinations,
  repeated logging controls, picker search/results, finish actions, toggles, and
  segmented controls.
- Reduced motion still communicates state changes.
- Haptic-disabled mode still has visible feedback.
- Press, selected, focused, and checked feedback follows the visible component
  shape and does not glow into adjacent content.

## Validation Update Contract

When this feature completes a previously deferred manual gate, update the older
feature validation file with:

- A reference to `specs/011-milestone-ux-hardening/validation/milestone-results.md`.
- A short status summary.
- Any remaining deferral reason if the gate is only partially closed.

Do not duplicate large evidence blocks across older specs.
