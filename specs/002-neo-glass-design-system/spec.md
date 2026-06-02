# Feature Specification: Neo-Glass Design System

**Feature Branch**: `002-neo-glass-design-system`

**Created**: 2026-05-29

**Status**: Implemented

**Input**: User description: "Create a Compose Multiplatform design system that is performant and easy to use across Android and iOS for a fitness app focused on ease of use and execution. Intuitive UX, large touch targets, easy to read, accessible. Easy to theme with some flair — components that don't look average. Prefer lower-level code over libraries. Use tokens for spacing, heights, margins, colors, shapes, etc. so things change in one place. Balance ease of use and UX with surprise and delight via animation and haptics."

> Migrated from the superpowers design spec + Plans 1–5 (now removed). The implemented module is `:design-system` (`com.jjswigut.oopsallprs.ds`); the committed code is the source of truth.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Re-skin the App From One Place (Priority: P1)

A developer changes the app's entire look — or toggles light/dark — by swapping a single
palette object, with no hunt-and-replace through screens. Every visual constant (spacing,
size, color, shape, type, glow, motion) is a token read from `FitTheme`.

**Why this priority**: Token-driven theming is the foundation every component and screen
depends on; without it, "change in one place" and the distinctive identity are impossible.

**Independent Test**: Wrap content in `FitTheme(palette = FitPalettes.IceDark)`, read tokens via
`FitTheme.colors/spacing/...`, switch to `IceLight`, and confirm the tree re-skins with an
animated color transition and no per-call-site edits.

**Acceptance Scenarios**:

1. **Given** content wrapped in `FitTheme`, **When** a different `FitPalette` is provided, **Then** all token consumers re-skin and color tokens animate to their new values.
2. **Given** either reference palette, **When** body text is rendered on a surface, **Then** the text/surface contrast meets WCAG AA (verified by test).
3. **Given** a component reads `FitTheme.spacing.lg`, **When** the spacing token value changes in one place, **Then** every consumer reflects it.

### User Story 2 - Build Accessible Gym UI From Ready Components (Priority: P1)

A developer composes screens from a catalog of ready components (buttons, toggles, chips,
sliders, cards, sheets, dialogs, list rows, text fields, steppers, segmented controls, tab bar,
top bar, progress ring, rest timer, stat tile, set counter, start button) that are large-target,
labeled for screen readers, reduce-motion aware, and haptic-with-alternative by default.

**Why this priority**: The app's core workout flows must be usable one-handed, under fatigue,
and accessibly; baking this into components prevents per-screen drift.

**Independent Test**: Place any interactive component in a test harness and confirm its target is
≥ `touchMin` (56dp), it exposes the correct role/state semantics, and motion collapses under
reduce-motion.

**Acceptance Scenarios**:

1. **Given** any interactive component, **When** measured, **Then** its touch target is ≥ 56dp.
2. **Given** `LocalReduceMotion` is true, **When** an animated component renders, **Then** motion collapses to a quick fade/instant state.
3. **Given** `LocalHapticsEnabled` is false, **When** an interaction fires, **Then** no haptic plays and the visual feedback still occurs.

### User Story 3 - Distinctive, Performant Neo-Glass Surfaces (Priority: P1)

Surfaces render the dark-first, luminous, translucent "Neo-Glass" look (gradient fill, luminous
border, inner sheen, outer glow) without the runtime backdrop-blur performance trap, so lists
scroll smoothly.

**Why this priority**: The product differentiates on a distinctive look; it must not cost
frame rate or obscure the next action.

**Independent Test**: Apply `Modifier.fitGlass()` to a surface inside a scrolling list and confirm
it renders the glass treatment with no runtime backdrop blur.

**Acceptance Scenarios**:

1. **Given** a glass surface, **When** rendered, **Then** it uses gradient fill + luminous border + inner sheen + a cheap expanded-outline glow, not `Modifier.blur` of live content.
2. **Given** the design system, **When** its dependencies are inspected, **Then** it does not depend on `material3`.

### User Story 4 - Tactile Number Entry (Priority: P2)

A user sets a weight or rep count with a tactile rolling picker: drag to roll through values with
a haptic detent per notch and a fling that snaps to the nearest value; configurable step, range,
and unit; exposed as an adjustable accessibility node.

**Why this priority**: Fast, satisfying number entry is central to gym-side logging and is the
design system's signature "surprise and delight" component.

**Independent Test**: Drive `FitRoller`/`FitWeightRoller`/`FitRepRoller` and verify value math
(`RollerMath`) maps index↔value and clamps to range; selection changes report new values.

**Acceptance Scenarios**:

1. **Given** a roller with min/max/step, **When** values are generated, **Then** the inclusive list and index↔value mapping are correct (verified by `ComponentMathTest`).
2. **Given** a roller, **When** the centered value changes, **Then** `onValueChange` fires and a `Selection` haptic plays.

### Edge Cases

- Reduce-motion + haptics-disabled simultaneously: all feedback degrades to visual only.
- Bundled font absent: `FitType` falls back to the platform font (current state; bundled font deferred).
- Light-mode glass: frosted-white treatment with softened glows (separate hand-tuned palette).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Ship a Material-free Kotlin/Compose Multiplatform `:design-system` module targeting Android + iOS, built only on `compose.runtime/foundation/ui/animation`.
- **FR-002**: Provide typed token groups: `FitSpacing`, `FitShapes`, `FitSize`, `FitColors`, `FitGlow`, `FitType`, `FitMotion`, plus a WCAG `contrastRatio` utility.
- **FR-003**: Provide `FitTheme` exposing all token groups via `CompositionLocal`, with a `FitTheme.*` accessor and animated palette/light-dark swap.
- **FR-004**: Provide reference palettes `IceDark` and `IceLight`, both meeting WCAG AA for body text on surface and ≥3.0 for muted/accent labels.
- **FR-005**: Provide foundation primitives: `Modifier.glass`/`fitGlass` (faux-glass), `Modifier.pressable` (spring + haptic, reduce-motion aware), and `AnimatedCount` (rolling tabular digits).
- **FR-006**: Provide a cross-platform haptics abstraction (`HapticType`, `HapticFeedback`, `LocalHaptics`, `expect rememberHapticFeedback`) with Android and iOS `actual`s, gated by `LocalHapticsEnabled`.
- **FR-007**: Provide the component catalog: `FitButton`/`FitIconButton`, `FitToggle`, `FitChip`, `FitSlider`, `FitCard`, `FitListRow`, `FitDialog`, `FitSheet`, `FitTextField`, `FitStepper`, `FitSegmentedControl`, `FitTabBar`, `FitTopBar`, `FitRoller`/`FitWeightRoller`/`FitRepRoller`, `FitProgressRing`, `FitRestTimer`, `FitStatTile`, `FitSetCounter`, `FitStartButton`.
- **FR-008**: All interactive components MUST use ≥ `touchMin` targets, set semantics, and respect reduce-motion and haptics-enabled flags.
- **FR-009**: Pure logic (contrast, palette, roller/stepper/slider math, digit formatting) MUST be covered by JVM-runnable unit tests.

### Key Entities *(include if feature involves data)*

- **Token groups**: immutable data classes (`FitColors`, `FitSpacing`, `FitShapes`, `FitSize`, `FitGlow`, `FitType`, `FitMotion`).
- **FitPalette**: `name`, `isDark`, `colors: FitColors`.
- **HapticType**: `Tick, Light, Medium, Heavy, Success, Warning, Selection`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `:design-system` compiles for Android (`compileDebugKotlinAndroid`) and iOS (`compileKotlinIosSimulatorArm64`).
- **SC-002**: `:design-system:testDebugUnitTest` passes (tokens, contrast, palettes, component math).
- **SC-003**: Both reference palettes pass programmatic WCAG-AA contrast assertions.
- **SC-004**: The module has no `material3` dependency.
- **SC-005**: A full re-skin or light/dark toggle requires changing only the `palette` argument.

## Assumptions

- The bundled signature font (e.g. Space Grotesk) is deferred; `FitType` defaults to the platform font and accepts an injected `FontFamily` later.
- Instrumented `runComposeUiTest` UI tests are deferred; verification uses JVM logic tests + compile on both targets.
- Device-level visual tuning (roller centering, glass glow) is expected before screens ship on top.

## Constitution Alignment *(mandatory)*

- **VII (Single token-driven design system, accessible gym UX)**: This feature *is* the official Neo-Glass token-driven system mandated by the amended Principle VII (constitution v3.0.0); Material 3 is intentionally not used.
- **VI (Shared-first, platform-respectful)**: All UI in shared Compose; only haptics use `expect`/`actual` adapters.
- **VIII (Quality gates)**: Compile-on-both-targets + JVM unit tests recorded under `validation/`.
