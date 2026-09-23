# Store Listing Assets

Generated store-listing artwork for Oops All PRs.

## Google Play

- `google-play/listing/en-US/*.txt`: approved English title and descriptions
- `google-play/icon-512x512.png`: Play icon derived from the production launcher icon
- `google-play/feature-graphic-1024x500.png`
- `google-play/screenshots/*.png` at 1080x1920
- `google-play/seven-inch-tablet-screenshots/*.png`: runtime captures from a 7-inch Android emulator
- `google-play/ten-inch-tablet-screenshots/*.png`: runtime captures from a 10.1-inch Android emulator

## App Store

- `app-store/iphone-6-9/*.png` at 1320x2868

## Source

- `source/oops-dumbbell-cereal-backdrop.png`: AI-generated dumbbell-cereal backdrop used for compositing.
- Screenshots are sourced from validation captures under `specs/*/validation/screenshots` and the current production active-workout capture in `source/current-active-workout-set-entry.png`.
- `source/current-active-workout-set-entry.png` is the API 36 production-build
  first-set-entry proof captured on 2026-09-22. It is used by the first Google
  Play and App Store panel so the storefront shows the current `Finish workout`
  flow without an on-screen keyboard.
- Tablet screenshots are direct captures of the current Android app; their emulator configuration and capture date are documented alongside the files.
