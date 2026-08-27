import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

const WORKFLOW_URL = new URL('../../.github/workflows/build.yml', import.meta.url);

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

    assert.match(lintJob, /run: pnpm lint/);
    assert.match(testJob, /needs: lint/);
    assert.match(testJob, /run: pnpm test/);
    assert.match(testJob, /run: pnpm dup/);
    assert.match(testJob, /run: pnpm audit --audit-level=low/);
    assert.match(buildJob, /needs: test/);
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
