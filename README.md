# homeflix-native

The native Homeflix frontend: React Native (Expo SDK 57, expo-router,
JavaScript ESM). Profile gate, home with recommendation billboard and
Continue Watching, per-library grids, search, and detail pages against the
Jellyfin API. Playback uses a custom `expo-video` player and the server-owned
Homeflix playback pipeline.

Roadmap: iOS first, Android later, TV after that. Every dependency must run
on Android too.

## Dev loop

```bash
pnpm install
pnpm start            # Metro; press i for the iOS simulator (Expo Go)
pnpm test             # node:test over src/**/*.test.js
pnpm check            # tests + audit (the gate)
```

Playback uses native Expo modules. Run the development build with `pnpm ios`.
The app restores portrait orientation when the player closes.

## Playback

`src/playback/` contains the custom controls, pipeline progress screen, source
negotiation, signed source release, reporting, recovery, and separate audio and
subtitle selectors. Source and track policy runs on the Jellyfin server. The
API contract lives in the local-flix repository at `docs/playback-api.md`.

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

## Server resolution

`src/session/serverResolver.js` selects the configured LAN or private network Jellyfin
address.
