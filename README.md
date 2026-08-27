# homeflix-native

Homeflix native clients live in separate application roots. Each app owns its
runtime, dependencies, source, native projects, and build output.

```text
apps/
  ios/
  android/
  android-tv/
```

The iOS client is in `apps/ios`. Android work belongs in `apps/android`, and
television-specific Android work belongs in `apps/android-tv`. Neither Android
root inherits the iOS Expo setup.

## iOS commands

Root commands delegate to the `@homeflix/ios` workspace package:

```bash
pnpm install
pnpm start
pnpm ios
pnpm test
pnpm check
```

See `apps/ios/README.md` for iOS development, playback, fixtures, and unsigned
build instructions.
