<!--
Sync Impact Report
Version change: 2.0.0 -> 3.0.0
Modified principles:
- VII. Material 3, Accessible Gym UX -> VII. Single Token-Driven Design System,
  Accessible Gym UX
  Rationale for MAJOR bump: Principle VII is redefined in a backward-incompatible
  way. The Material 3 component-semantics mandate is removed and replaced with a
  requirement to build all UI from one shared, token-driven design system (the
  "Neo-Glass" Fit design system; see docs/superpowers/specs). All accessibility
  and gym-UX requirements are preserved.
Added principles: none
Added sections: none
Removed sections: none
Templates updated in this amendment:
- ✅ .specify/templates/plan-template.md (Constitution Check wording)
- ✅ .specify/templates/spec-template.md (mandatory-section label)
- ✅ .specify/templates/tasks-template.md (T009 + verification task wording)
- ✅ .specify/templates/checklist-template.md (gate wording)
- ✅ AGENTS.md (reviewed; no changes required)
Follow-up TODOs:
- None
-->
# Oops All PRs Constitution

## Core Principles

### I. Fast-Loop Logging First

The primary product experience is recording a workout while the user is in the
gym. Active set logging MUST minimize taps, avoid modal interruption, keep
controls thumb-friendly, and preserve a clear next action. The normal active
logging path MUST be stepper-first or one-tap-first. Freeform text entry MAY
exist only as an escape hatch for direct numeric correction, search, naming, or
custom exercise creation. Features that add friction to confirming the next set
MUST prove the added value is worth the added interaction cost.

Rationale: Oops All PRs competes on speed and confidence during a workout, not
on planning complexity.

### II. Workout Data Is a Ledger

Confirmed workout work MUST be durable, timestamped, and auditable. A set MUST
not appear logged until persistence succeeds. A logged set MUST keep its
`loggedAt` record and MUST only be changed through an explicit edit path that
preserves the fact that it was logged. Finishing a workout MUST keep completed
sets and discard unlogged planned sets without mutating logged set tuples.
Workout lifecycle states MUST distinguish reusable routines, active workouts,
and completed history, regardless of the final storage model.

Rationale: Users trust the app with training history and PR evidence. Silent
mutation, duplicate logging, or lost sets directly breaks that trust.

### III. Session Recovery Is Core

Active workouts, elapsed workout time, rest timers, and active navigation state
MUST recover after app restart, process death, and ordinary backgrounding.
Timers MUST use wall-clock anchors such as start instants and rest end instants,
not countdown deltas as the source of truth. Rest completion notifications MUST
be scheduled when a rest begins or changes, and canceled on skip, finish, or
discard. Any feature that touches active session state MUST include recovery
acceptance criteria.

Rationale: A workout often spans locked screens, interruptions, and OS process
management. The app must not punish the user for putting the phone away.

### IV. Progress Is the Product Promise

Personal records, last-set context, history, and progression views are core
features. PR detection MUST handle weighted, bodyweight, fractional, and unit
converted sets correctly. Bodyweight movements MUST be loggable, exportable,
and PR-eligible with reps alone; added weight MAY be represented as optional
load. Progress features MUST explain what improved and preserve enough
historical data to make the claim inspectable.

Rationale: The app is named Oops All PRs. The reward loop is not secondary
analytics; it is the emotional center of the product.

### V. Local-First, Sync-Ready Ownership

Workout data MUST be usable offline and stored locally by default. Network
features and cloud sync are out of scope until introduced by a future
constitution amendment and feature spec. The architecture MUST avoid choices
that make future sync impractical: domain entities need stable identifiers,
timestamps, explicit mutation paths, and conflict-aware boundaries. Export,
share, backup, and restore flows MUST be user-visible and user-controlled.
Android Auto Backup is allowed, but backup behavior MUST be documented before
public release.

Rationale: The current product promise is private, fast, local workout logging,
while future sync remains likely enough to protect the data model now.

### VI. Shared-First, Platform-Respectful Architecture

The product is a first-class Kotlin Multiplatform app. Android is the first
execution target for implementation and testing, and iOS MUST remain a
first-class product target in architecture, domain logic, and shared UI.
Business rules, models, validation, repositories, use cases, and most Compose
UI MUST live in shared code. Platform code MUST stay limited to adapters such
as database drivers, notifications, haptics, file sharing, permissions, and
system integration. Android-only shortcuts in shared logic are constitution
violations unless they are isolated behind expect/actual or equivalent
platform interfaces.

Rationale: Android-first sequencing gives fast validation, while shared-first
boundaries prevent an Android-only rewrite from blocking iOS later.

### VII. Single Token-Driven Design System, Accessible Gym UX

User-facing UI MUST be built from one shared, token-driven design system — the
"Neo-Glass" Fit design system (see `docs/superpowers/specs`) — and MUST NOT
introduce parallel or ad hoc hardcoded styling. All spacing, sizing, color,
shape, typography, elevation/glow, motion, and haptic values MUST come from
design tokens (`FitTheme`) so they can change in one place; raw style literals
at call sites are violations unless the needed token is genuinely absent and is
being added to the token set in the same change. Material 3 component semantics
are NOT required; the design system defines its own components and MAY diverge
visually. Platform conventions for navigation, dialogs, back behavior, and
system integration MUST still be respected on both Android and iOS.

Core workout flows MUST support large touch targets (at least the system
`touchMin`), TalkBack/VoiceOver labels and state descriptions, readable dynamic
type, a reduced-motion path, haptic feedback with non-haptic alternatives, and
one-handed gym ergonomics. Motion and visual polish MUST support speed and
clarity; decoration MUST NOT obscure the next logging action.

Rationale: The product deliberately differentiates on a distinctive, performant
visual identity rather than stock Material. Concentrating every visual decision
in one token system keeps that identity consistent and themeable while still
guaranteeing the accessibility and gym usability the product requires under real
gym conditions: sweat, fatigue, one hand, and quick glances.

### VIII. Public-Release Quality Gates

Every behavior-changing feature MUST include automated verification scaled to
its risk. Session, ledger, PR, export, and persistence changes MUST include
state or integration tests. Active workout changes MUST include PRD acceptance
coverage where applicable. Public release gates MUST include Android build and
unit tests, shared KMP build health, release artifact checks, permissions and
backup review, asset validation, data export sanity checks, accessibility
review, and milestone or release manual device verification for core workout
flows. Per-PR manual verification is optional unless the feature plan marks it
as required.

Rationale: Public app-store quality requires more than compiling. The highest
risk areas are user data integrity, gym-side usability, accessibility, and
release artifacts.

## Product Scope and Domain Rules

Oops All PRs is a fast strength-training logger focused on quick workout
recording and visible progress. The primary workflows are starting or resuming
a workout, logging sets, managing rest, finishing or discarding, reviewing
history, seeing PRs and progression, and exporting user-owned data.

Users MUST be able to start from a reusable routine, create routines from prior
workouts, start an empty workout, and add exercises during a workout. The
constitution does not require templates to be stored as workout rows or as a
separate entity; the chosen model MUST preserve those user workflows and keep
active logging distinct from routine editing.

Exercise seed data MAY be imported from CSV. The current OopsAllPRs exercise
set is the baseline seed unless a future plan identifies a more comprehensive
source with clear licensing, better classification coverage, and migration
steps. Seed ingestion MUST validate required fields, avoid duplicate canonical
exercise names, preserve user-created exercises, and support bodyweight
classification.

Weights MUST have a canonical storage unit. User-selected units, fractional
weights, locale decimal input, display formatting, export formatting, and
bodyweight reps-only logging MUST be handled at the appropriate boundaries.

## Technical Direction

The default architecture is Kotlin Multiplatform with shared domain, data,
state, and Compose UI; Android and iOS provide platform adapters. Local storage
MUST use SQLDelight or an equivalent typed persistence layer with explicit
migrations and tests for schema changes. Repository and use-case layers MUST
return explicit success or failure signals for user-data mutations; silent
exception swallowing and empty-data fallbacks are not acceptable for core data
paths.

The fresh implementation MUST avoid preserving known debt as architecture:
large catch-all database helpers, mixed navigation ownership, direct dependency
lookups inside composables, naive CSV parsing, and implementation-lore comments
as a substitute for specs or tests. Derived PR and last-set data MAY be
materialized or computed on demand, but the plan MUST document the tradeoff,
recalculation rules, and migration impact.

## Development Workflow

Specs MUST describe observable user value, fast-loop impact, data integrity
requirements, recovery behavior, progress behavior, platform scope, and release
quality evidence. Plans MUST explicitly state Android-first and iOS implications
for every shared or platform feature. Tasks MUST be organized by independently
deliverable user stories, with separate foundational tasks for shared domain,
persistence, recovery, and platform adapters.

The Constitution Check in each plan is a gate before Phase 0 research and again
after Phase 1 design. Any violation MUST be recorded in Complexity Tracking with
the user impact, simpler alternative rejected, and a removal or revisit point.
Milestone and release checklists MUST include manual verification for affected
core workout flows on real devices.

## Governance

This constitution supersedes conflicting project conventions, generated
templates, and informal workflow notes. Amendments MUST be proposed as a
documented change set that includes the updated constitution, a Sync Impact
Report, version bump rationale, and required updates to templates or runtime
guidance. Every implementation plan, task list, and review MUST verify
compliance with the current constitution before work is considered complete.

Versioning follows semantic versioning. MAJOR changes remove or redefine
principles in a backward-incompatible way. MINOR changes add principles,
sections, or materially expanded governance. PATCH changes clarify wording,
correct errors, or make non-semantic refinements. The ratification date remains
the original adoption date; the last amended date changes whenever the
constitution text changes.

**Version**: 3.0.0 | **Ratified**: 2026-05-29 | **Last Amended**: 2026-05-29
