# Implementation Plan: Neo-Glass Design System

**Branch**: `002-neo-glass-design-system` | **Date**: 2026-05-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-neo-glass-design-system/spec.md`

**Status**: Implemented and committed (`:design-system` module, commit `aa9509c`).

## Summary

A Material-free Compose Multiplatform design system for Oops All PRs. Visual direction is
**Neo-Glass Depth** (dark-first, luminous, translucent), rendered with a cheap faux-glass recipe
(no runtime backdrop blur). Everything is token-driven through `FitTheme`, so spacing, size,
color, shape, type, glow, and motion change in one place and palettes (incl. light/dark) swap
with a single argument. Rich motion + cross-platform haptics are built into the components,
including the signature tactile `FitRoller`.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2; AGP 8.9.0;
compileSdk 35; minSdk 31 (matches the existing project catalog).

**Primary Dependencies**: `compose.runtime`, `compose.foundation`, `compose.ui`,
`compose.animation`, and `kotlinx-coroutines-core` (for the rest-timer tick). **No `material3`.**
Two catalog entries were added: `compose-ui`, `compose-animation`.

**Storage**: N/A (UI library).

**Testing**: Pure-logic JVM tests via the Android target's `testDebugUnitTest`
(`TokenSanityTest`, `ContrastTest`, `FitPaletteTest`, `ComponentMathTest`). Compile verification
on Android + iOS. Instrumented `runComposeUiTest` UI tests deferred.

**Target Platforms**: Android (first execution/validation) + iOS (`iosX64`, `iosArm64`,
`iosSimulatorArm64`). Haptics are the only `expect`/`actual` boundary.

## Module Structure

```
design-system/
  build.gradle.kts                 // KMP, Material-free; android jvmTarget 1_8; ios targets
  src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/
    token/      FitSpacing, FitShapes, FitSize, FitColors, FitGlow, FitType, FitMotion, Contrast
    theme/      FitPalette, FitPalettes (IceDark/IceLight), FitTheme (+ CompositionLocals)
    foundation/ Glass (Modifier.glass), FitGlass (Modifier.fitGlass), Pressable
    motion/     AnimatedCount
    haptic/     Haptics (HapticType, HapticFeedback, LocalHaptics, expect factory)
    component/  FitButton, FitToggle, FitChip, FitSlider, FitCard, FitListRow, FitDialog,
                FitSheet, FitTextField, FitStepper, FitSegmentedControl, FitTabBar, FitTopBar,
                FitRoller (+RollerMath, FitWeightRoller, FitRepRoller), FitProgressRing,
                FitRestTimer, FitStatTile, FitSetCounter, FitStartButton
  src/androidMain/kotlin/.../haptic/Haptics.android.kt   // View.performHapticFeedback
  src/iosMain/kotlin/.../haptic/Haptics.ios.kt           // UIKit feedback generators
  src/commonTest/kotlin/.../  TokenSanityTest, ContrastTest, FitPaletteTest, ComponentMathTest
```

## Key Design Decisions

- **CompositionLocal token system** (not Material extension, not global singletons): swappable,
  preview-friendly, no Material look leakage.
- **Faux-glass** instead of real backdrop blur everywhere; real blur reserved for a future modal
  scrim only. `FitSheet` currently uses a dim scrim.
- **`FitMotion`** stores damping/stiffness and exposes generic `*Spec<T>()` builders plus Float
  convenience accessors, so Color/Dp/Float animations all type-check from one source of feel.
- **Single signature accent + semantic states**; reference palettes `IceDark`/`IceLight`.

## Constitution Check

- **Principle VII**: This module is the mandated single token-driven design system; PASS.
- **Principle VI**: UI shared; haptics behind `expect`/`actual`; PASS.
- **Principle VIII**: Compile-on-both-targets + JVM unit tests; recorded in `validation/`.

## Deltas From Original Plans (superpowers Plans 1–5)

- Package is `com.jjswigut.oopsallprs.ds` (not the draft `fit.ds`).
- No fresh Gradle root scaffold — reused the existing project root, wrapper, and catalog.
- Bundled font deferred; `FitType` defaults to the platform font.
- `compose.uiTest`/instrumented source set not added; verification is JVM logic tests + compile.

## Complexity Tracking

No constitution violations to track. The one intentional scope deferral (bundled font,
instrumented UI tests) is documented above and in the spec Assumptions.
