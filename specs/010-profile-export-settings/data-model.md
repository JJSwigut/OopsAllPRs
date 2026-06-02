# Data Model: Profile Settings and Local Export

## ProfileState

Represents the shared state rendered by the Profile destination.

**Fields**:

- `weightUnit`: selected display/export unit.
- `paletteMode`: current runtime palette selection.
- `hapticsEnabled`: current runtime haptic preference.
- `reduceMotion`: current runtime motion preference.
- `localStatus`: readable local-first status lines for Profile.
- `isHydrated`: whether durable preferences have been loaded.
- `isExporting`: whether an export request is in progress.
- `lastExport`: latest successful export result for the current app session.
- `exportError`: latest recoverable export error message.

**Validation rules**:

- `weightUnit` is always one of the supported canonical display units.
- `lastExport` and `exportError` are mutually exclusive after a completed
  export request.
- `isExporting` must return to false after success or failure.

## ProfileExportResult

Summarizes a generated export for user feedback.

**Fields**:

- `type`: export category requested by the user.
- `fileName`: generated file name.
- `rowCount`: number of data rows, excluding headers.
- `weightUnit`: unit used when formatting weight-bearing data.

**Validation rules**:

- `rowCount` can be zero.
- `fileName` must be non-empty on success.
- Bodyweight-only records may have no weight value in content while still
  counting as exported rows.

## LocalOwnershipStatus

Read-only Profile copy explaining the app's data ownership posture.

**Fields**:

- `storageLabel`: local/offline storage status.
- `syncLabel`: current sync/account status.
- `backupLabel`: platform backup eligibility status.

**Validation rules**:

- Must not imply cloud sync is active.
- Must remain accurate while the device is offline.

## Export Request

Transient action produced by tapping an export control.

**Fields**:

- `type`: one of workouts, routines, exercises, or personal records.
- `unit`: the currently selected display unit at request time.

**State transitions**:

```text
Idle -> Exporting -> Success(lastExport)
Idle -> Exporting -> Failure(exportError)
Failure -> Exporting -> Success(lastExport)
Success -> Exporting -> Success(lastExport)
```

## Persistence Relationships

- `ProfileState.weightUnit` loads from and saves to the local preference row.
- Export requests read local workout/routine/exercise/PR repositories through
  the export repository and record export snapshots there.
- Runtime palette/haptics/reduced-motion preferences remain in navigation/app
  shell state for this slice.
