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

test('should keep each native application in its own project root', async () => {
    const iosPackage = await readJson('apps/ios/package.json');
    const iosLayout = await readFile(new URL('apps/ios/app/_layout.js', ROOT_URL), 'utf8');
    const androidMarker = await readFile(new URL('apps/android/README.md', ROOT_URL), 'utf8');
    const androidTvMarker = await readFile(new URL('apps/android-tv/README.md', ROOT_URL), 'utf8');

    assert.equal(await exists('package.json'), false);
    assert.equal(await exists('pnpm-lock.yaml'), false);
    assert.equal(await exists('pnpm-workspace.yaml'), false);
    assert.equal(await exists('.npmrc'), false);
    assert.equal(await exists('patches'), false);
    assert.equal(await exists('apps/ios/pnpm-lock.yaml'), true);
    assert.equal(await exists('apps/ios/pnpm-workspace.yaml'), true);
    assert.equal(await exists('apps/ios/.npmrc'), true);
    assert.equal(iosPackage.name, '@homeflix/ios');
    assert.equal(iosPackage.main, 'expo-router/entry');
    assert.equal(iosPackage.scripts.start, 'expo start');
    assert.equal(iosPackage.scripts.ios, 'expo run:ios');
    assert.match(iosLayout, /export default function RootLayout/);
    assert.match(androidMarker, /^# Android$/m);
    assert.match(androidTvMarker, /<h1 align="center">Homeflix for Android TV<\/h1>/);
});

test('should keep Android TV as an independent native application', async () => {
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

    assert.equal(await exists('apps/android-tv/gradlew'), true);
    for (const module of modules) {
        assert.match(settings, new RegExp(`"${module}",`));
    }
    assert.deepEqual(await Promise.all(moduleBuilds.map(exists)), moduleBuilds.map(() => true));
});
