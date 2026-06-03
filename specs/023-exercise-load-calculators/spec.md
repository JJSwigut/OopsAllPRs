# Feature Specification: Exercise Load Calculators

**Feature Branch**: `codex/023-exercise-load-calculators`

**Created**: 2026-06-02

**Status**: Draft

**Input**: User description: "Create specific calculators for barbell and dumbbell exercises. Barbell calculator starts with bar weight and tapping plates adds that plate to both sides. Dumbbell calculator lets users tap standard dumbbell weights."

## User Scenarios & Testing

### User Story 1 - Calculate Barbell Loads

As a lifter logging a barbell exercise, I can open an EZ calculator from the set weight field, choose the bar, tap plate values, and apply the total load to the set.

**Acceptance Scenarios**

- Given a weighted barbell exercise and pounds as the current unit, when I open the calculator and tap `25`, then the total is `95 lb` for a 45 lb bar.
- Given a weighted barbell exercise and kilograms as the current unit, when I use a 20 kg bar and tap `25`, then the total is `70 kg`.
- Given multiple taps of the same plate, when I apply the calculator, then each tap adds one matching pair of plates.

### User Story 2 - Quickly Log Dumbbell Loads

As a lifter logging a dumbbell exercise, I can tap a standard dumbbell weight and have that per-hand value written to the current set.

**Acceptance Scenarios**

- Given a dumbbell exercise, when I tap `50 lb`, then the set weight becomes `50 lb`, not `100 lb`.
- Given the app is set to kilograms, when I tap a kilogram dumbbell preset, then the set stores the converted internal kilogram value.

### User Story 3 - Keep Other Exercise Types Unchanged

As a lifter logging bodyweight, timed, cable, or machine movements, I do not see irrelevant calculator controls.

**Acceptance Scenarios**

- Given a bodyweight or timed set, when the set input is shown, then no load calculator button is shown.
- Given weighted equipment that is not barbell or dumbbell, when the set input is shown, then no load calculator button is shown.

## Requirements

- **FR-001**: The active workout set input MUST show a calculator affordance only for weighted exercises whose equipment maps to barbell or dumbbell.
- **FR-002**: Exercise selection and active workout recovery MUST preserve enough equipment information to determine calculator eligibility.
- **FR-003**: Barbell calculation MUST compute `bar weight + 2 * sum(selected plate values)`.
- **FR-004**: Dumbbell calculation MUST record the selected dumbbell value per hand.
- **FR-005**: Calculator presets MUST follow the current app weight unit preference.
- **FR-006**: Applying a calculator value MUST update the current draft set's weight without automatically logging the set.
- **FR-007**: Existing manual weight entry, weight steppers, bodyweight sets, and timed sets MUST continue to work.

## Success Criteria

- **SC-001**: Users can produce a 95 lb barbell load by tapping a 25 lb plate once on a 45 lb bar.
- **SC-002**: Users can produce a 70 kg barbell load by tapping a 25 kg plate once on a 20 kg bar.
- **SC-003**: Users can select a dumbbell preset and see the same per-dumbbell weight in the draft set field.
- **SC-004**: Active workouts reloaded from local persistence retain calculator availability for exercises added with barbell/dumbbell equipment.

## Constitutional Alignment

- **Evidence-Based Fitness**: The feature improves accurate load logging without changing progression or record calculation semantics.
- **Local-First Ownership**: Equipment snapshots are stored locally with active workout state for recovery.
- **Platform Scope**: Implementation is in shared Compose/KMP code and applies to Android-first shared UI.
- **Design System & Accessibility**: Calculator controls use existing Fit components and content descriptions for the entry button.
- **Release Evidence**: Common model tests and SQL recovery tests cover calculator math, unit conversion, eligibility, and persistence.

## Assumptions

- V1 supports barbell and dumbbell calculators only.
- Presets are fixed common values and are not user-customizable.
- Dumbbell values are logged per dumbbell.
- Existing weight step preferences remain separate from calculator presets.
