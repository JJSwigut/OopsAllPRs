# Contracts: Exercise Progress Charting

## Exercise Detail Chart Contract

When a user selects an exercise from Progress:

- The exercise detail must show a chart section before or near the existing trend list.
- The chart section must show available metrics as a compact selector.
- The selected metric must filter chart points and the visible trend rows.
- The chart must render empty, single-point, and multi-point states without layout breakage.
- The chart must show latest value context in text, not only visually.
- The chart must use FitTheme tokens and avoid Material or external charting components.

## Metric Contract

Supported metric labels:

- `BEST_SET`: Best set
- `ESTIMATED_ONE_REP_MAX`: e1RM
- `VOLUME`: Volume
- `BODYWEIGHT_REPS`: Bodyweight reps

Metric availability is per exercise and based on existing local progress points.

## Evidence Contract

For chart/trend point selection:

- Source workout id and source set id must be preserved where present.
- Selecting a point with source evidence must route to existing Progress evidence.
- Missing local source data must show the existing unavailable-source message.
- Charting must not modify PRs, progress points, completed workouts, or active workouts.

## Layout Contract

On a Pixel 9 Pro viewport:

- Chart content must remain inside the Progress scroll area.
- Bottom resume banner and bottom navigation must not overlap chart controls.
- Labels must fit or wrap without clipping.
- The chart line must remain visible against the current Fit palette.
