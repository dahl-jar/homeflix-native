import assert from 'node:assert/strict';
import { access, readdir } from 'node:fs/promises';
import { test } from 'node:test';

const ROOT_URL = new URL('../../', import.meta.url);
const COMPONENTS_URL = new URL('apps/ios/src/components/', ROOT_URL);
const PLAYBACK_URL = new URL('apps/ios/src/playback/', ROOT_URL);
const PLAYBACK_RESPONSIBILITIES = [
    'controls',
    'device',
    'episodes',
    'pipeline',
    'player',
    'runtime',
    'session-monitoring',
    'skip-segments',
    'sources',
    'telemetry',
    'tracks',
    'video',
];

async function exists(url) {
    try {
        await access(url);
        return true;
    } catch {
        return false;
    }
}

async function directJavaScriptFiles(url) {
    const entries = await readdir(url, { withFileTypes: true });
    return entries
        .filter((entry) => entry.isFile() && entry.name.endsWith('.js'))
        .map((entry) => entry.name)
        .sort();
}

test('should keep shared components in matching folders without barrels', async () => {
    const entries = await readdir(COMPONENTS_URL, { withFileTypes: true });
    const folders = entries.filter((entry) => entry.isDirectory()).map((entry) => entry.name);

    assert.deepEqual(await directJavaScriptFiles(COMPONENTS_URL), []);
    for (const folder of folders) {
        assert.equal(await exists(new URL(`${folder}/${folder}.js`, COMPONENTS_URL)), true);
        assert.equal(await exists(new URL(`${folder}/index.js`, COMPONENTS_URL)), false);
    }
});

test('should keep API and playback code inside responsibility folders', async () => {
    const apiUrl = new URL('apps/ios/src/api/', ROOT_URL);

    assert.deepEqual(await directJavaScriptFiles(apiUrl), []);
    assert.deepEqual(await directJavaScriptFiles(PLAYBACK_URL), []);
    assert.equal(await exists(new URL('__tests__/', apiUrl)), false);
    assert.equal(await exists(new URL('__tests__/', PLAYBACK_URL)), false);
});

test('should name playback folders after their responsibilities', async () => {
    const entries = await readdir(PLAYBACK_URL, { withFileTypes: true });
    const folders = entries
        .filter((entry) => entry.isDirectory())
        .map((entry) => entry.name)
        .sort();

    assert.deepEqual(folders, PLAYBACK_RESPONSIBILITIES);
});
