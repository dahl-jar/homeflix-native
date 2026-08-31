import assert from 'node:assert/strict';
import { access, readFile } from 'node:fs/promises';
import { test } from 'node:test';

const ROOT_URL = new URL('../../', import.meta.url);

async function readJson(relativePath) {
    const contents = await readFile(new URL(relativePath, ROOT_URL), 'utf8');
    return JSON.parse(contents);
}

async function exists(relativePath) {
    try {
        await access(new URL(relativePath, ROOT_URL));
        return true;
    } catch {
        return false;
    }
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

test('should keep Android TV as an independent native application', async () => {
    const rootPackage = await readJson('package.json');
    const settings = await readFile(new URL('apps/android-tv/settings.gradle.kts', ROOT_URL), 'utf8');
    const modules = [
        ':app',
        ':core:catalog',
        ':core:designsystem',
        ':core:network',
        ':core:session',
        ':feature:auth',
        ':feature:detail',
        ':feature:home',
        ':feature:library',
        ':feature:player',
        ':feature:profile',
    ];
    const moduleBuilds = modules
        .slice(1)
        .map((module) => `apps/android-tv/${module.slice(1).replace(':', '/')}/build.gradle.kts`)
        .concat('apps/android-tv/app/build.gradle.kts');

    assert.equal(rootPackage.scripts['android-tv:build'], 'cd apps/android-tv && ./gradlew assembleDebug');
    assert.equal(rootPackage.scripts['android-tv:install'], 'cd apps/android-tv && ./gradlew installDebug');
    assert.equal(rootPackage.scripts['android-tv:lint'], 'cd apps/android-tv && ./gradlew ktlintCheck detekt lintDebug');
    assert.equal(rootPackage.scripts['android-tv:test'], 'cd apps/android-tv && ./gradlew testDebugUnitTest');
    for (const module of modules) {
        assert.match(settings, new RegExp(`"${module}",`));
    }
    assert.deepEqual(await Promise.all(moduleBuilds.map(exists)), moduleBuilds.map(() => true));
});
