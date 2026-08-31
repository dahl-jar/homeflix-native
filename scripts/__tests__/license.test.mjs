import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

const ROOT_URL = new URL('../../', import.meta.url);
const LICENSE_ID = 'MPL-2.0';

async function json(path) {
    return JSON.parse(await readFile(new URL(path, ROOT_URL), 'utf8'));
}

test('should declare MPL 2.0 across repository metadata', async () => {
    const iosPackage = await json('apps/ios/package.json');
    const license = await readFile(new URL('LICENSE', ROOT_URL), 'utf8');
    const readme = await readFile(new URL('README.md', ROOT_URL), 'utf8');
    const androidTvReadme = await readFile(new URL('apps/android-tv/README.md', ROOT_URL), 'utf8');

    assert.equal(iosPackage.license, LICENSE_ID);
    assert.match(license, /^Mozilla Public License Version 2\.0/);
    assert.match(readme, /\[Mozilla Public License 2\.0\]\(LICENSE\)/);
    assert.match(androidTvReadme, /\[Mozilla Public License 2\.0\]\(\.\.\/\.\.\/LICENSE\)/);
});

test('should exclude showcase media from the project license', async () => {
    const notice = await readFile(new URL('NOTICE.md', ROOT_URL), 'utf8');

    assert.match(notice, /docs\/images/);
    assert.match(notice, /not covered by the Mozilla Public License 2\.0/);
});
