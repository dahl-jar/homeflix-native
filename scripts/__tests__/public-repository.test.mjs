import assert from 'node:assert/strict';
import { access, readFile, readdir } from 'node:fs/promises';
import { test } from 'node:test';

const ROOT_URL = new URL('../../', import.meta.url);
const AUTH_MOCKS_URL = new URL('apps/ios/src/api/auth/__tests__/mocks/', ROOT_URL);
const ITEM_MOCKS_URL = new URL('apps/ios/src/api/items/__tests__/mocks/', ROOT_URL);
const RECOMMENDATIONS_URL = new URL(
    'apps/ios/src/features/home/__tests__/mocks/recommendations.json',
    ROOT_URL
);
const CAPTURE_SCRIPT_URL = new URL('apps/ios/scripts/capture-fixtures.sh', ROOT_URL);

test('should keep public fixtures synthetic', async () => {
    const authFiles = await readdir(AUTH_MOCKS_URL);
    const authText = await readFile(new URL('users-public.json', AUTH_MOCKS_URL), 'utf8');
    const users = JSON.parse(authText);
    const recommendations = JSON.parse(await readFile(RECOMMENDATIONS_URL, 'utf8'));

    assert.deepEqual(authFiles, ['users-public.json']);
    assert.deepEqual(users.map(({ Name, Id }) => ({ Name, Id })), [
        { Name: 'Darrow', Id: 'user-one' },
        { Name: 'Mustang', Id: 'user-two' },
        { Name: 'Goblin', Id: 'user-three' }
    ]);
    assert.doesNotMatch(authText, /ServerId|LastLoginDate|LastActivityDate|[a-f0-9]{32}/i);
    assert.deepEqual(recommendations.map(({ ItemId, Rank }) => ({ ItemId, Rank })), [
        { ItemId: 'item-one', Rank: 1 },
        { ItemId: 'item-two', Rank: 2 },
        { ItemId: 'item-three', Rank: 3 },
        { ItemId: 'item-four', Rank: 4 },
        { ItemId: 'item-five', Rank: 5 },
        { ItemId: 'item-six', Rank: 6 },
        { ItemId: 'item-seven', Rank: 7 },
        { ItemId: 'item-eight', Rank: 8 },
        { ItemId: 'item-nine', Rank: 9 },
        { ItemId: 'item-ten', Rank: 10 }
    ]);
    await assert.rejects(access(ITEM_MOCKS_URL));
    await assert.rejects(access(CAPTURE_SCRIPT_URL));
});
