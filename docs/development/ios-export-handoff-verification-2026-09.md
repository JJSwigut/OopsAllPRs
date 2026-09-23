# iOS Export Handoff Verification

Date: 2026-09-21. Local, uncommitted work in `swiggy/launch-integration`; no
release, store, account, telemetry, or user-data state changed.

## Finding

`FileExportHandoff` on iOS previously accepted an export request and did
nothing. Profile export is a user-visible ownership feature, so a silent
no-op on one platform is not an acceptable fallback.

## Change

- `IosAppViewControllerFactory` now passes its host controller to
  `FileExportHandoff`.
- `FileExportHandoff` writes the requested export content to a temporary file
  and presents `UIActivityViewController` with that file URL.
- The existing iOS backup handoff already uses the same controller-presentation
  boundary and temporary-file approach for document export.

## Verification And Limits

`./gradlew --no-daemon :shared:compileKotlinIosSimulatorArm64` passed. This
compiles the iOS Kotlin/Native source containing the new UIKit handoff.

Native Swift archive and simulator installation are now covered by the iOS 27
release package and launch gates. The native share-sheet interaction remains
unverified; before promotion, verify a CSV export opens the native share sheet
with the expected filename and readable contents.

### Failure-Surface Follow-Up (2026-09-22)

`FileExportHandoff` now throws when no `UIViewController` presenter is
available or when its temporary export file cannot be prepared. `ProfileStateHolder`
already converts handoff exceptions into its visible recoverable export error,
so iOS no longer reports a successful export after a silent native no-op. The
state behavior is covered by `ProfileStateHolderTest`, and
`:shared:compileKotlinIosSimulatorArm64` passed. This is not proof that the
native share sheet is presented; that interaction still needs an unlocked
simulator UI run.
