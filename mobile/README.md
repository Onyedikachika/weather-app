# Mobile apps (Android + iOS)

Capacitor wrapper around [`../webapp/`](../webapp/) — same HTML/CSS/JS, running inside a native
WebView shell so it installs and launches like a regular Android/iOS app.

`www/` is a generated copy of `../webapp/`, not source — don't edit it directly. Run
`npm run sync-web` after changing anything in `webapp/` to pull the latest files in and
re-sync the native projects.

## Android

Prerequisites (already set up on this machine):

- JDK 21 (`brew install openjdk@21` — Gradle/AGP don't yet support newer JDKs)
- Android SDK command-line tools (`brew install --cask android-commandlinetools`), with
  `platform-tools`, `platforms;android-36`, and `build-tools;36.0.0` installed via `sdkmanager`
- `android/local.properties` pointing `sdk.dir` at the SDK root

Build a debug APK:

```bash
npm run build:android
# output: android/app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device/emulator with `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`.

For a signed release build (Play Store), you'll need to generate a keystore and configure
signing in `android/app/build.gradle` — not set up yet.

## iOS

Scaffolded (`ios/App/App.xcodeproj`) but **not built** — this machine only has the Xcode
Command Line Tools, not full Xcode, and Xcode can't be installed non-interactively.

To build:

1. Install Xcode from the Mac App Store, then `sudo xcode-select -s /Applications/Xcode.app`
2. `npm run sync-web` (or just `npx cap open ios`, which does this first)
3. In Xcode: pick a simulator or your device, set a Signing Team under
   App target → Signing & Capabilities (a free Apple ID works for simulator/local device
   runs; a paid Apple Developer account is required to distribute via TestFlight/App Store)
4. Cmd+R to build and run

## App identity

- Bundle/package ID: `com.onyedikachika.weatherapp`
- Display name: "Weather App"

Change these via `capacitor.config.json` (`appId`/`appName`) plus the native project settings
(`android/app/build.gradle` `applicationId`, and the iOS target's Bundle Identifier in Xcode),
then `npx cap sync`.
