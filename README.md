# homeflix-native

The native Homeflix frontend: React Native (Expo SDK 57, expo-router,
JavaScript ESM). Profile gate, home with recommendation billboard and
Continue Watching, per-library grids, search, and detail pages against the
Jellyfin API. Playback is a stub (`src/playback/playerLauncher.js`); the
custom player replaces that module and nothing else.

Roadmap: iOS first, Android later, TV after that. Every dependency must run
on Android too.

## Dev loop

```bash
pnpm install
pnpm start            # Metro; press i for the iOS simulator (Expo Go)
pnpm test             # node:test over src/**/*.test.js
pnpm check            # tests + audit (the gate)
```

On a real iPhone: install Expo Go from the App Store, scan the QR from
`pnpm start` (same network as the Mac).

## Fixtures

Tests run against captured real server responses in
`src/api/__tests__/fixtures/`. Refresh them with:

```bash
HOMEFLIX_PIN=<owen pin> ./scripts/capture-fixtures.sh
```

The script scrubs the access token and fails if a live token leaks into a
fixture.

## Standalone build (sideload)

CI builds an unsigned `Homeflix.ipa` artifact on every push to main
(`.github/workflows/build.yml`). Download it from the Actions run and sign
with Sideloadly/AltStore. Locally:

```bash
pnpm exec expo prebuild --platform ios
cd ios && pod install && cd ..
xcodebuild build -workspace ios/Homeflix.xcworkspace -scheme Homeflix \
  -configuration Release -sdk iphoneos CODE_SIGNING_ALLOWED=NO
```

## Audit ignores

`pnpm-workspace.yaml` ignores three advisories that live in dev-time build
tooling only (Metro's `image-size`, the xcode config plugin's `uuid`) and
have no patched release compatible with Expo SDK 57. None of that code ships
in the app binary. Re-check on every SDK bump and drop the ignores once
upstream releases fixes.

## Server endpoints

`src/session/serverResolver.js`: LAN `http://homeflix.invalid:8096`, private network
`http://homeflix.invalid:8096` (needs private network when away from home).
