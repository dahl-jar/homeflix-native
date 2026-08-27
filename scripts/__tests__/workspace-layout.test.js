import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

const ROOT_URL = new URL('../../', import.meta.url);

async function readJson(relativePath) {
    const contents = await readFile(new URL(relativePath, ROOT_URL), 'utf8');
    return JSON.parse(contents);
}

test('should keep each native application in its own workspace root', async () => {
    const rootPackage = await readJson('package.json');
    const iosPackage = await readJson('apps/ios/package.json');
    const iosLayout = await readFile(new URL('apps/ios/app/_layout.js', ROOT_URL), 'utf8');
    const androidMarker = await readFile(new URL('apps/android/README.md', ROOT_URL), 'utf8');
    const androidTvMarker = await readFile(new URL('apps/android-tv/README.md', ROOT_URL), 'utf8');

    assert.equal(rootPackage.scripts.start, 'pnpm --filter @homeflix/ios start');
    assert.equal(rootPackage.dependencies, undefined);
    assert.equal(iosPackage.name, '@homeflix/ios');
    assert.equal(iosPackage.main, 'expo-router/entry');
    assert.match(iosLayout, /export default function RootLayout/);
    assert.match(androidMarker, /^# Android$/m);
    assert.match(androidTvMarker, /^# Android TV$/m);
});
