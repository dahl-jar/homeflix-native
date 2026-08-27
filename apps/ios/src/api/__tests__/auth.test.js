import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';

import { toGateCard, authenticate } from '../auth.js';

const publicUsers = JSON.parse(
    readFileSync(new URL('./fixtures/users-public.json', import.meta.url), 'utf8')
);

test('should decode public users fixture into gate cards', () => {
    const cards = publicUsers.map(toGateCard);

    const owen = cards.find((card) => card.name === 'owen');
    assert.ok(owen.id.length > 0);
    assert.equal(owen.hasPassword, true);
    const passwordless = cards.filter((card) => card.hasPassword === false);
    assert.ok(passwordless.length >= 2);
});

test('should send empty password for passwordless profiles', async () => {
    const bodies = [];
    const client = {
        post: async (path, body) => {
            bodies.push({ path, body });
            return { AccessToken: 'access-token-placeholder', User: { Id: 'id-one' } };
        }
    };

    await authenticate(client, 'seb');

    assert.equal(bodies[0].body.Pw, '');
});
