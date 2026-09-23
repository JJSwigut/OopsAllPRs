---
name: "wifi-install"
description: "Use when the user wants to connect to a Pixel or Android device over Wi-Fi with ADB, pair wireless debugging, install the debug APK, launch the app, or deploy Oops All PRs without USB."
---

# Wi-Fi Install

Use the repo helper script for wireless Android deployment:

```bash
tools/pixel_adb_wifi.sh <command>
```

## Workflow

1. If the user provides a pairing address/code and main wireless debugging address, run:

   ```bash
   tools/pixel_adb_wifi.sh pair <pair-ip:port> <pair-code> <connect-ip:port>
   ```

2. For normal reconnect/install/launch, run:

   ```bash
   tools/pixel_adb_wifi.sh deploy
   ```

   The helper first reuses an authorized connection, then tries the saved address,
   then discovers the paired Pixel's current `_adb-tls-connect` address through
   ADB mDNS. Android may rotate the port without requiring another pairing.

3. If only reconnecting is needed, run:

   ```bash
   tools/pixel_adb_wifi.sh connect
   ```

4. For diagnostics, run:

   ```bash
   tools/pixel_adb_wifi.sh status
   ```

## Notes

- The script discovers already-paired devices through ADB mDNS and stores the last successful wireless address in `.env.pixel-adb` as a fallback; this file is ignored by `.gitignore`.
- Do not save pairing codes. They are short-lived and should only be used for the immediate `pair` command.
- If pairing fails with a protocol error, restart ADB and try `connect <main-ip:port>`; the device may already be authorized.
- A changed wireless-debugging port should be found automatically while the Pixel is on the same network with Wireless debugging enabled.
- Pair again only if the Pixel no longer lists this Mac under **Paired devices**, the host ADB keys were replaced, or Android's wireless-debugging authorization was reset.
- The helper defaults to installing `:androidApp:installDebug` and launching `com.jjswigut.oopsallprs.android.debug`.
