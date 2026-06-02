# Research: Bootstrap Oops All PRs App Foundation

## Decision: Use Kotlin Multiplatform With Android-First Execution

**Rationale**: The existing app already uses a shared KMP module with Android
and iOS adapters. The constitution requires iOS to remain first-class, while
the user wants Android working first for testing speed. Keeping domain, data,
validation, and most UI in `shared` protects iOS while allowing Android to be
the first executable target.

**Alternatives considered**:
- Android-only rewrite: faster short-term but violates first-class iOS and
  would require rework for shared domain and adapters.
- Separate native Android/iOS apps: higher maintenance and slower foundation
  delivery.
- Desktop/web-first foundation: not aligned with gym-side mobile use.

## Decision: Use SQLDelight for Typed Local Persistence

**Rationale**: SQLDelight matches the existing app, supports KMP drivers, gives
typed queries, and lets tests exercise actual SQLite behavior. The feature is
local-only/offline-first and needs explicit schema migrations, stable IDs, and
ledger integrity. SQLDelight supports that without introducing a network or ORM
layer.

**Alternatives considered**:
- Room: strong Android choice but not first-class KMP/iOS for shared persistence.
- Plain SQLite wrappers: flexible but loses typed query safety and migration
  clarity.
- Object stores/DataStore-only: not suitable for relational workout, set, PR,
  routine, and seed data.

## Decision: Model Routines Separately From Active and Completed Workouts

**Rationale**: The user does not require a specific storage shape, but does
require creating routines from workouts, launching routines, starting empty, and
adding exercises on the fly. Separate domain concepts avoid accidental edits to
saved plans while allowing planning to choose tables that share common exercise
and set shapes.

**Alternatives considered**:
- Store routines only as `WorkoutStatus.TEMPLATE` rows: simpler and matches the
  current app, but risks conflating active ledger history with reusable plans.
- Store only completed workouts and clone from history: insufficient for
  reusable routines that evolve independently of history.

## Decision: Canonical Weight Storage Uses Kilograms

**Rationale**: The existing app stores weights canonically in kilograms and
converts at UI/export boundaries. Kilograms are stable for international use,
support fractional values, and reduce ambiguity for future sync/export.

**Alternatives considered**:
- Store values in the user's display unit: simpler input path but breaks
  consistency when users change units and complicates future sync.
- Store both pounds and kilograms: duplicate source of truth and invites drift.

## Decision: Bodyweight Sets Are Reps-First With Optional Added Load

**Rationale**: The user explicitly wants bodyweight movements to avoid required
weight entry. The constitution requires bodyweight sets to be loggable,
exportable, and PR-eligible with reps alone. Added load remains optional for
weighted pull-ups, dips, and similar cases.

**Alternatives considered**:
- Encode bodyweight as zero kilograms in all contexts: simple but can obscure
  intent unless set type/bodyweight semantics are explicit.
- Require weight for every set: rejected because it blocks common calisthenics
  logging.

## Decision: Use Wall-Clock Anchors for Active Sessions and Timers

**Rationale**: The existing app audits identified timer drift and process-death
problems. The constitution requires started-at and rest-ends-at anchors as the
truth. Persisting anchors lets UI recalculate elapsed/remaining time after
backgrounding or process death.

**Alternatives considered**:
- Persist countdown deltas: fails under process death and OS doze.
- In-memory timers only: fails cold-start recovery.

## Decision: Exercise Seed Source Is Existing OopsAllPRs CSV Baseline

**Rationale**: The user asked to use the same exercises unless a better licensed
source is identified. Discovery found `shared/src/commonMain/resources/exercises.csv`
with 591 exercise rows plus header. Use this as baseline and improve validation,
deduplication, parser safety, and seed versioning rather than sourcing unknown
data.

**Alternatives considered**:
- External exercise database: deferred until licensing, coverage, and migration
  can be evaluated.
- Hardcoded seed list in Kotlin: harder to audit, update, and validate.
- Keep current naive CSV parser behavior: rejected because it uses basic comma
  splitting and weak validation.

## Decision: Derived PR and Last-Set Data Are Rebuildable From Ledger Evidence

**Rationale**: PR and progress are core, but the source of truth must remain
logged workout evidence. Materialized tables may be used for performance, but
they must be rebuildable and traceable to source workouts and sets.

**Alternatives considered**:
- Store only materialized PRs: faster reads but risks stale or uninspectable
  progress claims.
- Compute all progress on every screen load: simplest correctness story but
  may not scale with history size.

## Decision: Explicit Failure Results for Core Mutations

**Rationale**: The current code has several catch-and-empty/null patterns in
database helpers. The constitution rejects silent failure for core data paths.
Repository and use-case methods that mutate workouts, sets, routines, seed data,
or exports must return success/failure outcomes or throw domain-specific errors
that callers handle.

**Alternatives considered**:
- Continue catch/log/empty patterns: rejected because they can hide data loss.
- Use exceptions everywhere without typed results: acceptable for internal
  boundaries only if callers map failures to explicit UI/domain outcomes.

## Decision: Milestone Manual Verification, Not Per-PR Manual Verification

**Rationale**: The user chose milestone/release gates. Automated tests gate
individual behavior changes, while manual device validation focuses on
milestone and release readiness for gym-side flows.

**Alternatives considered**:
- Per-PR manual device checks: stronger but slower than requested.
- Release-only manual checks: too late for foundational session and ledger UX.

