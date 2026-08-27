# Homeflix iOS

The iOS Homeflix client uses React Native, Expo SDK 57, Expo Router, and
JavaScript ESM. It provides the profile gate, home recommendations, library
grids, search, details, and the custom Homeflix playback pipeline.

## Development

Run these commands from the repository root:

```bash
pnpm install
pnpm start
pnpm ios
pnpm test
pnpm check
```

`pnpm start` launches Metro. `pnpm ios` runs the native development build. The
player restores portrait orientation when it closes.

## Playback

`src/playback/` contains the player controls, progress pipeline, source
negotiation, signed source release, reporting, recovery, audio selection, and
subtitle selection. The Jellyfin server owns source and track policy. The API
contract lives in the local-flix repository at `docs/playback-api.md`.

See `docs/player-controls-and-simulator.md` for controls and iOS Simulator
validation.

## Fixtures

Tests use captured server responses from `src/api/__tests__/fixtures/`. Refresh
them from the repository root:

```bash
HOMEFLIX_PIN=<pin> apps/ios/scripts/capture-fixtures.sh
```

The script scrubs access tokens and fails if a live token reaches a fixture.

## Unsigned build

CI builds `Homeflix.ipa` on pushes to `main`. To build the same unsigned app
locally from the repository root:

```bash
pnpm --filter @homeflix/ios exec expo prebuild --platform ios
cd apps/ios/ios && pod install && cd ../../..
xcodebuild build \
  -workspace apps/ios/ios/Homeflix.xcworkspace \
  -scheme Homeflix \
  -configuration Release \
  -sdk iphoneos \
  -derivedDataPath apps/ios/build \
  CODE_SIGNING_ALLOWED=NO
```

## Dependency audit

The root `pnpm-workspace.yaml` ignores three advisories in development build
tools. Recheck them on each Expo SDK update and remove an ignore when its
upstream fix is available.

## Server resolution

`src/session/serverResolver.js` selects the configured LAN or private network Jellyfin
address.
