# Retention and Monetization Direction

**Access date:** 2026-09-21

**Status:** Current-market research and proposed experiments. This document does
not change pricing, entitlements, telemetry, or store metadata.

## Primary Sources

- [Strong](https://www.strong.app/) positions itself as a simple, intuitive
  tracker, with logging, templates, progress, export, cross-device access, and
  advanced tools such as RPE and custom timers.
- [Boostcamp Pro](https://www.boostcamp.app/pro) states that the full tracker,
  programs, RPE/RIR, rest timers, PRs, weekly reports, and custom program
  builder remain free. It sells advanced analytics, personalized programs, and
  exclusive coach content for $59.99/year or $14.99/month.
- [Boostcamp's tracker overview](https://www.boostcamp.app/workout-tracker)
  emphasizes rapid first-set logging, last-session values, automatic rest,
  progressive overload, and a weekly review as the core repeat loop.
- [Fitbod membership](https://app.prod.fitbod.me/) lists $95.99/year and
  $15.99/month for adaptive workouts. Its
  [FAQ](https://fitbod.me/faqs/) frames the paid value as personalized workouts,
  recovery/equipment adaptation, coaching cues, and connected-device support.
- [Fitbod subscriptions](https://help.fitbod.me/hc/en-us/sections/1500000506081-Subscriptions)
  notes a limited lifetime membership, but its ordinary offer is recurring.

These are product claims and prices from accessible official pages, not an
independent test of the competitors' current mobile binaries. Prices and store
availability vary by platform and region.

## What This Means for Oops All PRs

The defensible near-term wedge is not a generic exercise library or an opaque
strength score. It is a trustworthy, local-first training notebook that makes a
repeat session easier than remembering one: fast capture, accurate contextual
defaults, transparent progress evidence, recovery from mistakes, and an obvious
next session.

The current product already supports much of that wedge:

1. Configurable logging captures weighted, bodyweight, timed, and mixed
   exercises without making every workout a special case.
2. Previous values are visible as suggestions, not hidden targets.
3. Circuits, rest timing, post-workout correction, and saved templates reduce
   friction in real training rather than only in a chart.
4. The Evidence Ladder keeps granular PR celebrations separate from a broader
   progress interpretation.

The release candidate should not claim that it replaces adaptive programming,
coaching video, or a large programs marketplace. Those are the principal paid
value layers competitors advertise, and this app does not currently offer them.

## Pricing Observation

The existing one-time lifetime unlock should be treated as a launch hypothesis,
not as a validated long-term business model. A $14.99 lifetime offer is easy to
understand and can remove purchase anxiety for early users, but it cannot by
itself establish durable revenue for ongoing cross-platform development,
support, and future cloud services. Boostcamp and Fitbod both place their
recurring price behind continuing optimization or adaptation, not behind basic
set logging.

Do not reduce the free experience below a trustworthy completed workout,
history, export, and recovery path merely to force conversion. That would
weaken the local-first promise and risk the core habit before it exists.

## Proposed Experiment Backlog

Ordered by information value and implementation risk.

1. **First-session activation study.** Recruit a small set of target lifters.
   Measure time to first logged set, successful completion, and whether they
   understand the suggested values and rest behavior. Use the existing neutral
   task protocol in `workout-summary-ux-2026-09.md`; do not infer retention from
   a single session.
2. **Weekly training recap usability study.** The local, transparent seven-day
   review is implemented from existing workouts: sessions completed, specific
   achievements, qualified Capability/Work capacity changes, and explicit
   insufficient-data states. No universal score, medical claim, or prescriptive
   programming. Test whether people can name a useful next action without
   assistance.
3. **Progressive-overload recommendation research.** Before implementation,
   define a conservative per-exercise recommendation contract that distinguishes
   an observed suggestion from a prescribed target and handles deloads,
   bodyweight loading, and user overrides. This is the clearest bridge from a
   tracker to recurring value, but it is not ready for a paid gate today.
4. **Entitlement packaging decision.** After activation evidence, choose one
   coherent offer: retain a clearly labelled early-adopter lifetime unlock, or
   introduce a subscription only when a continuing value layer exists. Avoid a
   hybrid menu until there is real willingness-to-pay evidence.
5. **Consentful funnel measurement.** If remote analytics is approved, define
   only decision-relevant events: first-set completion, workout completion,
   template reuse, weekly-review open, paywall view, purchase success, restore,
   and cancellation/error. Publish the privacy disclosure before collection.
   Local debug observations are not a substitute for consent or product data.

## Decision Gates

Before pricing or a public-release claim, establish:

- iOS build/runtime verification and both store sandbox purchase/restore proof.
- A privacy-policy and analytics decision, if any remote collection is desired.
- At least a documented first-session and repeat-session usability study.
- A defined recovery floor for unpaid users: human-readable export remains
  available. Import, restore, automatic backup, and sync remain Full Access
  features until the allowance model has durable identity.
- An explicit owner decision on whether the current lifetime price is a limited
  early-adopter offer or the intended permanent commercial model.

This is a product direction, not proof of retention, conversion, profitability,
or competitive advantage.
