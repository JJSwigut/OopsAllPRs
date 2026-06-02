---
description: "Task list for the Neo-Glass design system"
---

# Tasks: Neo-Glass Design System

**Input**: Design documents from `/specs/002-neo-glass-design-system/`

**Status**: All tasks implemented and committed (`:design-system`, commit `aa9509c`). Checkboxes
are marked complete; file paths point to the committed source (the source of truth).

**Tests**: Pure-logic verification via `testDebugUnitTest`; compile verification on Android + iOS.
Instrumented `runComposeUiTest` UI tests intentionally deferred (see plan.md).

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 Add `:design-system` to `settings.gradle.kts` and the catalog entries `compose-ui`, `compose-animation` in `gradle/libs.versions.toml`
- [X] T002 Create Material-free KMP module `design-system/build.gradle.kts` (Android + iOS targets, no `material3`)

## Phase 2: Token System (US1)

- [X] T003 [P] [US1] `token/FitSpacing.kt`, `token/FitShapes.kt`, `token/FitSize.kt` (≥56dp `touchMin`)
- [X] T004 [P] [US1] `token/Contrast.kt` (WCAG relative luminance + `contrastRatio`)
- [X] T005 [P] [US1] `token/FitColors.kt`, `token/FitGlow.kt`
- [X] T006 [P] [US1] `token/FitType.kt` (tabular numerals; injectable `FontFamily`, defaults to platform), `token/FitMotion.kt` (damping/stiffness + generic `*Spec<T>()` builders)

## Phase 3: Theming (US1)

- [X] T007 [US1] `theme/FitPalette.kt` + `theme/FitPalettes.kt` (IceDark / IceLight)
- [X] T008 [US1] `theme/FitTheme.kt` (CompositionLocals, `FitTheme.*` accessor, animated palette/light-dark swap, haptics/reduce-motion/haptics-enabled providers)

## Phase 4: Foundation Primitives (US2, US3)

- [X] T009 [US3] `foundation/Glass.kt` (`Modifier.glass` faux-glass) + `foundation/FitGlass.kt` (theme-aware `fitGlass`)
- [X] T010 [US2] `foundation/Pressable.kt` (`Modifier.pressable` — spring scale + haptic, reduce-motion aware)
- [X] T011 [US2] `motion/AnimatedCount.kt` (rolling tabular digits)

## Phase 5: Haptics (US2)

- [X] T012 [US2] `haptic/Haptics.kt` (`HapticType`, `HapticFeedback`, `LocalHaptics`, `expect rememberHapticFeedback`)
- [X] T013 [US2] `haptic/Haptics.android.kt` (`View.performHapticFeedback`) and `haptic/Haptics.ios.kt` (UIKit generators)

## Phase 6: Component Catalog (US2, US3, US4)

- [X] T014 [P] [US2] Controls: `FitButton`/`FitIconButton`, `FitToggle`, `FitChip`, `FitSlider`
- [X] T015 [P] [US3] Surfaces: `FitCard`, `FitListRow`, `FitDialog`, `FitSheet` (dim scrim)
- [X] T016 [P] [US2] Inputs & nav: `FitTextField`, `FitStepper`, `FitSegmentedControl`, `FitTabBar`, `FitTopBar`
- [X] T017 [US4] Hero roller: `FitRoller` (+ `RollerMath`, `FitWeightRoller`, `FitRepRoller`)
- [X] T018 [P] [US2] Fitness: `FitProgressRing`, `FitRestTimer`, `FitStatTile`, `FitSetCounter`, `FitStartButton`

## Phase 7: Verification (US1–US4)

- [X] T019 [US1] `ContrastTest`, `FitPaletteTest` (WCAG AA on both palettes), `TokenSanityTest`
- [X] T020 [US4] `ComponentMathTest` (RollerMath, stepper, slider, digit formatting)
- [X] T021 Compile both targets (`compileDebugKotlinAndroid`, `compileKotlinIosSimulatorArm64`) and run `testDebugUnitTest` — results in `validation/build-and-test-results.md`

## Deferred (tracked, not blocking)

- [ ] T022 Bundle the signature variable font and wire `rememberFitFontFamily()` into `FitType`
- [ ] T023 Add instrumented `runComposeUiTest` coverage for component semantics/interaction
- [X] T024 On-device visual tuning pass: roller centering + faux-glass glow
