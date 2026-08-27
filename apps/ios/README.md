# Homeflix for iOS

This package expects the custom Jellyfin API extensions provided by the Homeflix server.

## Run

Run these commands from the repository root:

```bash
cp apps/ios/.env.example apps/ios/.env.local
pnpm install
pnpm ios
```

Set `EXPO_PUBLIC_HOMEFLIX_SERVER_URLS` in `apps/ios/.env.local` to one or more comma-separated server URLs.

```bash
pnpm start
pnpm check
```

## Fixtures

```bash
HOMEFLIX_SERVER_URL=https://media.example.com \
HOMEFLIX_PIN=<pin> apps/ios/scripts/capture-fixtures.sh
```

Captured responses are stored in `src/api/__tests__/fixtures/`. The script removes access tokens and fails if a live token reaches a fixture.

## Unsigned build

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

CI packages the unsigned app as `Homeflix.ipa` on pushes to `main`.
