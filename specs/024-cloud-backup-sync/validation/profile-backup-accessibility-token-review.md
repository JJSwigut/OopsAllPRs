# Profile Backup Accessibility and Token Review

**Date**: 2026-06-05

## Scope

- Reviewed `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt`.
- Focused on the Backup card added for link, backup, sync, restore, conflict resolution, status, privacy, and error states.

## Result

- PASS: Backup controls use existing `FitButton` and `FitButtonStyle.Secondary` components.
- PASS: Status copy uses existing `StatusRow`, `FoundationMutedText`, and design-system spacing.
- PASS: Error and destructive replacement messages use `FitTheme.colors.danger`.
- PASS: Plain JSON privacy copy is visible in the Backup card.
- PASS: Busy state disables backup controls and changes the primary link button label to `Working`.
- PASS: Conflict actions are visible only when the state reports `CONFLICT` or `BACKUP_CHANGED`.

## Follow-Up For Manual QA

- Verify screen reader labels for the document picker result on physical Android/iOS devices.
- Verify long provider display names wrap correctly on narrow mobile widths.
