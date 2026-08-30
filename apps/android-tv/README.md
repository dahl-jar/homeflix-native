# Android TV

Homeflix for Android TV is a native Kotlin and Jetpack Compose application. It
builds independently from the Expo iOS application and shares no runtime or
package dependencies with the phone clients.

## Structure

```text
app/                 thin launcher and application composition
core/designsystem/   TV theme, dimensions, and focus visuals
core/network/        Jellyfin transport, server probing, and identity headers
core/session/        encrypted authentication session storage and validation
feature/auth/        profile selection, PIN input, and auth UI state
feature/home/        Home API mapping, policies, and D-pad Compose UI
```

Each feature owns its `src/main`, `src/test`, and `src/androidTest` source sets.
Features can depend on named core modules but do not import sibling features.
The app module composes features and owns top-level navigation.

## Requirements

- JDK 17 or newer for running Gradle
- Android SDK Platform 37 with minimum device API 23
- Android SDK Build-Tools 36.0.0
- An Android TV device or emulator for connected tests

The checked-in Gradle wrapper downloads Gradle 9.4.1. `local.properties` holds
the local Android SDK path and stays outside Git.

## Server configuration

The app probes the Jellyfin server candidates supplied at build time. Separate
multiple addresses with commas:

```sh
HOMEFLIX_SERVER_URLS=http://your-jellyfin-host:8096 pnpm android-tv:install
```

Gradle can receive the same value directly:

```sh
cd apps/android-tv
./gradlew -PhomeflixServerUrls=http://your-jellyfin-host:8096 installDebug
```

No server address or account data is stored in tracked source.

## Commands

Run from the repository root:

```sh
pnpm android-tv:check
pnpm android-tv:build
pnpm android-tv:install
pnpm android-tv:connected-test
pnpm android-tv:mutation
```

The check command runs ktlint, detekt with Android type resolution, Android
Lint, JVM tests, and the debug build. The mutation command runs scoped PIT
audits for design-system, network, session, auth, and Home policy and mapping
logic. The connected tests exercise focus, authentication, encrypted session
storage, Home navigation, and app routing on an emulator or device.

The app probes Jellyfin, renders every public profile returned by the API, and
authenticates passwordless or PIN-protected profiles. Successful sessions are
encrypted with Android Keystore and restored only after server validation.

Home follows the iOS application contract for recommendations, resume items,
user views, and recently added media. Its television layout uses a focus-driven
hero, horizontal media rails, visible playback progress, and D-pad selection.
Details and playback are the next feature boundary.
