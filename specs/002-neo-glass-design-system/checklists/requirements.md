# Quality Checklist: Neo-Glass Design System

Constitution gates (v3.0.0) and feature quality. `[X]` = met.

## Design system (Principle VII)

- [X] Single token-driven system; all visual values come from `FitTheme` tokens
- [X] No parallel/hardcoded styling system; **no `material3` dependency**
- [X] Re-skin / light-dark via one `palette` argument (animated swap)
- [X] Reference palettes `IceDark` + `IceLight` meet WCAG AA (tested)

## Accessible gym UX (Principle VII)

- [X] Interactive targets ≥ `touchMin` (56dp)
- [X] Components set role/state semantics; roller & slider `adjustable`
- [X] Reduce-motion path (`LocalReduceMotion`) collapses motion
- [X] Haptics gated by `LocalHapticsEnabled` with visual alternative
- [ ] Dynamic-type and TalkBack/VoiceOver verified on device (deferred to integration)

## Shared-first (Principle VI)

- [X] All UI in shared Compose
- [X] Only haptics use `expect`/`actual`
- [X] Compiles for Android and iOS

## Quality gates (Principle VIII)

- [X] Pure-logic unit tests pass on JVM
- [X] Compile verified on both targets; results recorded under `validation/`
- [ ] Instrumented UI tests (deferred — T023)

## Known deferrals

- [ ] Bundled signature font (T022) — currently platform font
- [ ] On-device visual tuning (T024)
