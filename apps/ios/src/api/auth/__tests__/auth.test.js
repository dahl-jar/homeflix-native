import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';

import { toGateCard, authenticate } from '../auth.js';

const publicUsers = JSON.parse(
    readFileSync(new URL('./mocks/users-public.json', import.meta.url), 'utf8')
);

test('should decode public users fixture into gate cards', () => {
    const cards = publicUsers.map(toGateCard);

    const darrow = cards.find((card) => card.name === 'Darrow');
    assert.equal(darrow.id, 'user-one');
    assert.equal(darrow.hasPassword, true);
    const passwordless = cards.filter((card) => card.hasPassword === false);
    assert.equal(passwordless.length, 2);
});

test('should send empty password for passwordless profiles', async () => {
    const bodies = [];
    const client = {
        post: async (path, body) => {
            bodies.push({ path, body });
            return { AccessToken: 'access-token-placeholder', User: { Id: 'id-one' } };
        }
    };

    await authenticate(client, 'Goblin');

    assert.equal(bodies[0].body.Pw, '');
});
