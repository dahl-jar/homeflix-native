# Homeflix for iOS

This package expects the Jellyfin-compatible API described in the root README.

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
