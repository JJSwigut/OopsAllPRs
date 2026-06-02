# Milestone Manual Gate

Status: Deferred as a release gate.

This gate requires a supported Android device or emulator session with manual timing and observation.

Required checks:
- Fresh install to first logged set completes in 30 seconds or less.
- Active workout restore completes in 2 seconds or less with logged set data intact.
- Finish workout and inspect completed history evidence.
- Review backup behavior, export behavior, permissions, accessibility basics, and release artifact assets.

Deferral note: skipped for the current implementation pass because no Android
device/emulator validation environment is available in this shell. This remains
required before public release.
