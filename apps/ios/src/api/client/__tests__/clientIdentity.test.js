import assert from 'node:assert/strict';
import { test } from 'node:test';

import { resolveClientIdentity } from '../clientIdentity.ts';

const generatedId = 'e9ce56b0-fc28-48f0-a407-146a89cf4d31';

test('should reuse the stored device identity', async () => {
    let writes = 0;
    const identity = await resolveClientIdentity({
        readDeviceId: async () => generatedId,
        writeDeviceId: async () => {
            writes += 1;
        },
        createDeviceId: () => 'unused',
        version: '2.4.1'
    });

    assert.deepEqual(identity, { deviceId: generatedId, version: '2.4.1' });
    assert.equal(writes, 0);
});

test('should persist one generated device identity when missing', async () => {
    let stored = null;
    let generations = 0;
    const dependencies = {
        readDeviceId: async () => stored,
        writeDeviceId: async (deviceId) => {
            stored = deviceId;
        },
        createDeviceId: () => {
            generations += 1;
            return generatedId;
        },
        version: '2.4.1'
    };

    const first = await resolveClientIdentity(dependencies);
    const second = await resolveClientIdentity(dependencies);

    assert.deepEqual(first, second);
    assert.equal(stored, generatedId);
    assert.equal(generations, 1);
});

test('should reject an unavailable app version', async () => {
    await assert.rejects(resolveClientIdentity({
        readDeviceId: async () => generatedId,
        writeDeviceId: async () => undefined,
        createDeviceId: () => generatedId,
        version: undefined
    }), /app version is unavailable/);
});

test('should reject an invalid generated device identity', async () => {
    await assert.rejects(resolveClientIdentity({
        readDeviceId: async () => null,
        writeDeviceId: async () => undefined,
        createDeviceId: () => 'shared-device',
        version: '2.4.1'
    }), /invalid UUID/);
});
