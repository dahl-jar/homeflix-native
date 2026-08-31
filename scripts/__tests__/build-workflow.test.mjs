import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

const WORKFLOW_URL = new URL('../../.github/workflows/build.yml', import.meta.url);
const ANDROID_TV_WORKFLOW_URL = new URL('../../.github/workflows/android-tv.yml', import.meta.url);

function job(workflow, name, nextName = null) {
    const start = workflow.indexOf(`  ${name}:\n`);
    const end = nextName ? workflow.indexOf(`  ${nextName}:\n`, start) : workflow.length;
    return workflow.slice(start, end);
}

test('should run lint test and build jobs in sequence', async () => {
    const workflow = await readFile(WORKFLOW_URL, 'utf8');
    const lintJob = job(workflow, 'lint', 'test');
    const testJob = job(workflow, 'test', 'build');
    const buildJob = job(workflow, 'build');

    assert.match(lintJob, /run: pnpm lint\n        working-directory: apps\/ios/);
    assert.match(lintJob, /run: pnpm typecheck\n        working-directory: apps\/ios/);
    assert.match(testJob, /needs: lint/);
    assert.match(testJob, /run: node --test "scripts\/\*\*\/\*\.test\.\*"/);
    assert.match(testJob, /run: pnpm test\n        working-directory: apps\/ios/);
    assert.match(testJob, /run: pnpm dup\n        working-directory: apps\/ios/);
    assert.match(testJob, /run: pnpm audit --audit-level=low\n        working-directory: apps\/ios/);
    assert.match(buildJob, /needs: test/);
});

test('should install iOS dependencies from the iOS project', async () => {
    const workflow = await readFile(WORKFLOW_URL, 'utf8');

    assert.equal(
        workflow.match(/run: pnpm install --frozen-lockfile\n        working-directory: apps\/ios/g)?.length,
        3
    );
});

test('should run the build workflow only for iOS and shared metadata changes', async () => {
    const workflow = await readFile(WORKFLOW_URL, 'utf8');

    assert.match(workflow, /push:\n    branches: \[main\]\n    paths:\n      - "apps\/ios\/\*\*"/);
    assert.doesNotMatch(workflow, /apps\/android-tv/);
});

test('should run the Android TV workflow only for Android TV changes', async () => {
    const workflow = await readFile(ANDROID_TV_WORKFLOW_URL, 'utf8');

    assert.match(workflow, /push:\n    branches: \[main\]\n    paths:\n      - "apps\/android-tv\/\*\*"/);
    assert.doesNotMatch(workflow, /apps\/ios/);
});

test('should publish a verified signed Android TV release', async () => {
    const workflow = await readFile(ANDROID_TV_WORKFLOW_URL, 'utf8');
    const releaseJob = job(workflow, 'release');

    assert.match(releaseJob, /permissions:\n      contents: write/);
    assert.match(releaseJob, /ANDROID_TV_KEYSTORE_BASE64/);
    assert.match(releaseJob, /ANDROID_TV_KEYSTORE_PASSWORD/);
    assert.match(releaseJob, /apksigner[\s\S]* sign/);
    assert.match(releaseJob, /apksigner[\s\S]* verify/);
    assert.match(releaseJob, /files: apps\/android-tv\/app\/build\/outputs\/apk\/release\/app-release\.apk/);
    assert.doesNotMatch(releaseJob, /files: .*app-release-unsigned\.apk/);
});

test('should grant workflow read-only repository access', async () => {
    const workflow = await readFile(WORKFLOW_URL, 'utf8');

    assert.match(workflow, /^permissions:\n  contents: read$/m);
});

test('should use actions with Node 24 runtimes', async () => {
    const workflow = await readFile(WORKFLOW_URL, 'utf8');

    assert.equal(workflow.match(/uses: actions\/checkout@v7/g)?.length, 3);
    assert.equal(workflow.match(/uses: actions\/setup-node@v7/g)?.length, 3);
    assert.equal(workflow.match(/uses: pnpm\/action-setup@v6/g)?.length, 3);
});
