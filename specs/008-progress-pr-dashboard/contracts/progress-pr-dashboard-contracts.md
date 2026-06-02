# Contracts: Progress and PR Dashboard V1

## Recent PR Contract

**Given** personal records exist,
**when** Progress is opened,
**then** recent PR rows appear in reverse chronological order.

**Given** a weighted record is shown,
**when** the user reads the row,
**then** the row identifies exercise, record kind, value, reps or estimate, and
display unit.

**Given** a bodyweight reps record is shown,
**when** the user reads the row,
**then** the row identifies reps without requiring a weight.

## Exercise Group Contract

**Given** records exist for multiple exercises,
**when** the user browses by exercise,
**then** each exercise group shows only records and trend points for that
exercise.

**Given** an exercise has no weighted records,
**when** its group is shown,
**then** weighted-only sections do not imply missing invalid data.

## Evidence Contract

**Given** a PR row has source ids,
**when** the user opens evidence,
**then** the source completed workout/set context is shown and the source set is
identifiable.

**Given** the source workout or set cannot be found,
**when** evidence is opened,
**then** an unavailable evidence state is shown without fabricated values.

## Trend Contract

**Given** progress points exist for an exercise,
**when** the exercise detail is shown,
**then** points appear in chronological order with metric labels, values, and
source ids when available.

**Given** weight values are displayed,
**when** the user-selected unit differs from canonical storage,
**then** labels use the display unit without changing stored evidence.

## Manual Review Contract

Manual milestone review must cover:
- Progress with no PRs.
- Progress with recent weighted and bodyweight PRs.
- Exercise grouping for at least two exercises.
- Source evidence for a weighted PR and a bodyweight PR.
- Missing evidence fallback if test data allows.
- Simple trend context for one exercise.
- Unit display for weighted records.

Passing review means Progress is readable, source evidence is trustworthy,
bodyweight reps-only records do not show required weight, and primary controls
do not overlap system bars or each other.
