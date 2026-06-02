# Feature Specification: Progress and PR Dashboard V1

**Feature Branch**: `codex/008-progress-pr-dashboard`

**Created**: 2026-05-30

**Status**: Draft

**Input**: User description: "Build feature 008 Progress and PR Dashboard V1. Show personal records as the core Progress experience, including weighted PRs, bodyweight reps PRs, estimated one-rep max, and volume records. Users should be able to see recent PRs, browse PRs by exercise, open the source completed workout/set evidence, and understand what improved in plain language. Keep everything local-only/offline-first, derive from completed workout ledger data, preserve traceability to source workout and set ids, support canonical weight units and bodyweight reps-only records, use the existing design-system module only, validate Android first, and keep iOS first-class through shared KMP state and Compose boundaries."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See Recent PRs First (Priority: P1)

A lifter opens Progress and immediately sees recent personal records with plain-language labels that explain what improved.

**Why this priority**: PRs are the product promise. The first Progress slice must make record-setting work visible and rewarding without requiring the user to interpret raw data.

**Independent Test**: A tester can complete record-setting weighted and bodyweight workouts, open Progress, and see recent PRs with exercise names, record kinds, values, and source timing.

**Acceptance Scenarios**:

1. **Given** completed workouts have generated personal records, **When** the user opens Progress, **Then** recent PRs appear before secondary progress content.
2. **Given** a weighted record exists, **When** it is shown, **Then** the label identifies the exercise, record type, reps or estimate, and display weight in the user's selected unit.
3. **Given** a bodyweight reps record exists, **When** it is shown, **Then** the label identifies reps without requiring or inventing a weight value.
4. **Given** there are no records yet, **When** Progress is opened, **Then** the empty state explains that PRs will appear after completed workouts without blocking other app sections.

---

### User Story 2 - Browse Records by Exercise (Priority: P2)

A lifter can browse Progress by exercise and see the records for one exercise grouped by meaningful record kinds.

**Why this priority**: Recent PRs are motivational, but lifters also need to answer "what are my bench records?" or "what is my pull-up best?".

**Independent Test**: A tester can create PRs for at least two exercises, open Progress, choose an exercise, and see only that exercise's records grouped by record kind.

**Acceptance Scenarios**:

1. **Given** records exist for multiple exercises, **When** Progress is opened, **Then** the user can choose an exercise from a local record-backed exercise list.
2. **Given** an exercise is selected, **When** its detail is shown, **Then** weighted, bodyweight, estimated one-rep max, and volume records are grouped in a scan-friendly way when present.
3. **Given** an exercise has only bodyweight records, **When** its detail is shown, **Then** weighted-only sections are omitted or shown as unavailable without implying missing data.
4. **Given** the app restarts, **When** Progress is opened, **Then** record-backed exercise browsing restores from local data.

---

### User Story 3 - Inspect Source Evidence (Priority: P3)

A lifter can open a PR's source evidence and see the completed workout and set that produced the record.

**Why this priority**: PRs must be trustworthy. Traceability to the original workout/set turns the dashboard from decoration into evidence.

**Independent Test**: A tester can open a PR row, navigate to the source completed workout context, and confirm the highlighted source set matches the record's source set id.

**Acceptance Scenarios**:

1. **Given** a PR row is visible, **When** the user opens it, **Then** the app shows the source completed workout detail or an equivalent evidence view.
2. **Given** source evidence is shown, **When** the relevant set is visible, **Then** the source set is identifiable and its reps/weight/bodyweight values match the PR row.
3. **Given** source workout data is missing or unavailable, **When** the user opens a PR, **Then** the app shows a non-destructive unavailable-evidence state instead of crashing or fabricating data.

---

### User Story 4 - Understand Progress Trends (Priority: P4)

A lifter can see simple local trend context for record-backed exercises, such as recent progress points or best values over time.

**Why this priority**: Trends add context after the PR dashboard is useful, but they should not delay the PR-first experience.

**Independent Test**: A tester can complete multiple workouts for one exercise and see locally derived progress points in chronological order for that exercise.

**Acceptance Scenarios**:

1. **Given** progress points exist for an exercise, **When** the exercise progress detail is shown, **Then** points are ordered chronologically and tied to source workout/set evidence when available.
2. **Given** an exercise has too little history for a trend, **When** its detail is shown, **Then** the app shows current records without implying a false trend.
3. **Given** records use canonical stored weights, **When** trend values are displayed, **Then** weight values respect the user's selected display unit without changing stored evidence.

### Edge Cases

- Weighted and bodyweight records for the same exercise remain distinct and do not overwrite each other.
- Fractional weights display without losing canonical precision.
- Estimated one-rep max and volume records explain the metric plainly enough to be useful without a formula screen.
- PR records remain traceable after app restart using local completed workout and set ids.
- Missing source workout/set data shows a safe unavailable-evidence state.
- Progress rows remain useful with one record, many records, no records, or records for only bodyweight exercises.
- Completed workout history, templates, active workout state, exercise catalog entries, and export-ready data are not changed by opening Progress or inspecting records.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Progress MUST prioritize personal records over secondary analytics or decorative content.
- **FR-002**: Progress MUST show recent personal records in reverse chronological order by achievement time.
- **FR-003**: Recent PR rows MUST include exercise name, record kind, value, achieved date or time context, and source availability.
- **FR-004**: Weighted PR display MUST support weight-for-reps, estimated one-rep max, and volume records using the user's display unit.
- **FR-005**: Bodyweight PR display MUST support reps-only records without requiring or inventing a weight value.
- **FR-006**: PR labels MUST explain what improved in plain language.
- **FR-007**: Progress MUST provide an empty state when no PR data exists.
- **FR-008**: Users MUST be able to browse records by exercise from local record data.
- **FR-009**: Exercise-specific record detail MUST group records by record kind.
- **FR-010**: Exercise-specific record detail MUST omit or clearly mark unavailable record kinds without implying invalid missing data.
- **FR-011**: Users MUST be able to open source evidence from a PR row.
- **FR-012**: Source evidence MUST preserve traceability to the source completed workout id and source set id.
- **FR-013**: Source evidence MUST show reps, weight when present, bodyweight reps-only values, and logged timing for the source set.
- **FR-014**: Missing source evidence MUST produce a visible unavailable-evidence state and MUST NOT fabricate workout or set data.
- **FR-015**: Progress trend data MUST be derived from local completed workout ledger and progress records.
- **FR-016**: Exercise trend points MUST be ordered chronologically and tied to source evidence when available.
- **FR-017**: Display-unit conversion MUST not change canonical stored evidence.
- **FR-018**: Progress data MUST restore from local storage after app restart without network access.
- **FR-019**: Opening Progress, browsing records, or inspecting evidence MUST NOT mutate completed workout history, templates, active workout state, exercise catalog entries, or exportable data.
- **FR-020**: The UI MUST support one-handed review on phone-sized screens with readable text, large touch targets, no overlapping primary controls, and assistive labels.

### Key Entities *(include if feature involves data)*

- **PR Dashboard State**: The Progress screen state containing recent PR rows, exercise groups, selected exercise, selected evidence, empty/error state, and loading status.
- **PR Row**: A display-ready personal record with exercise name, record kind, display value, source ids, achieved time, and plain-language improvement text.
- **Exercise PR Group**: All record rows and progress points for a single exercise.
- **PR Evidence**: A source completed workout/set projection used to prove where a PR came from.
- **Progress Trend Point**: A display-ready point for a metric over time with value, unit context, source ids, and recorded time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After completing record-setting weighted and bodyweight workouts, a tester can open Progress and identify the two newest PRs in under 10 seconds.
- **SC-002**: A tester can browse records by exercise and find all record kinds for a selected exercise without using instructions.
- **SC-003**: A tester can open a PR's source evidence and verify the source set values match the PR row.
- **SC-004**: Automated validation proves weighted, bodyweight reps-only, estimated one-rep max, and volume records render with stable source workout/set ids.
- **SC-005**: Automated validation proves display-unit conversion does not mutate canonical stored evidence.
- **SC-006**: After app restart, Progress still shows PR rows, exercise groupings, and source evidence without network access.
- **SC-007**: Android build, shared behavior tests, iOS shared compile, Material scan, and milestone manual review evidence are recorded for the feature.

## Assumptions

- Personal records and progress points are derived from completed workouts by existing or extended local PR derivation behavior.
- The dashboard can use display projections rather than new storage tables unless implementation discovers missing persisted evidence.
- Source evidence may reuse completed workout detail behavior from History when that satisfies traceability.
- Trend visualization can be a simple ordered list or compact visual summary for V1; full charting is out of scope.
- Editing/deleting PRs, workouts, progress points, and templates is out of scope for this slice.
- Android remains the first manual validation target; iOS remains first-class through shared domain, state, and Compose boundaries.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Progress is read-only and must not add friction to active workout logging; PR evidence should reinforce the reward loop after logging.
- **Ledger Integrity**: PR rows and evidence are derived from completed workout ledger data and preserve source workout/set traceability without mutating logged tuples.
- **Recovery Behavior**: Progress rows, exercise groupings, and evidence restore from local data after restart.
- **Progress Promise**: The feature directly centers PRs, weighted/bodyweight records, estimated one-rep max, volume, source evidence, and plain-language improvement.
- **Local-First Ownership**: All records, trends, and evidence work offline from local completed workout/progress data and remain export-ready.
- **Platform Scope**: Android is validated first while shared behavior and UI remain first-class for iOS.
- **Design System & Accessibility**: Progress UI uses the shared token-driven design system, large touch targets, readable type, assistive labels, reduced motion, and non-haptic alternatives.
- **Release Evidence**: Automated tests must cover record display, exercise grouping, source evidence, missing evidence, bodyweight records, display units, local restore, and non-mutation, with milestone manual review on a device when available.
