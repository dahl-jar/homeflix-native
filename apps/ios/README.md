# Homeflix for iOS

This package expects the Jellyfin-compatible API described in the root README.

## Run

```bash
cd apps/ios
cp .env.example .env.local
pnpm install
pnpm ios
```

Set `EXPO_PUBLIC_HOMEFLIX_SERVER_URLS` in `.env.local` to one or more comma-separated server URLs.

```bash
pnpm start
pnpm check
```

## Unsigned build

```bash
cd apps/ios
pnpm exec expo prebuild --platform ios
cd ios
pod install
xcodebuild build \
  -workspace Homeflix.xcworkspace \
  -scheme Homeflix \
  -configuration Release \
  -sdk iphoneos \
  -derivedDataPath ../build \
  CODE_SIGNING_ALLOWED=NO
```

CI packages the unsigned app as `Homeflix.ipa` on pushes to `main`.
