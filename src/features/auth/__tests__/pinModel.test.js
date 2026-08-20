import { test } from 'node:test';
import assert from 'node:assert/strict';

import { createPinModel } from '../pinModel.js';

test('should submit pin exactly on the fourth digit', () => {
    const model = createPinModel();

    assert.equal(model.append(6).submit, false);
    assert.equal(model.append(9).submit, false);
    assert.equal(model.append(9).submit, false);
    const fourth = model.append(0);
    assert.equal(fourth.submit, true);
    assert.equal(fourth.pin, '6990');
});

test('should clear pin after failed auth', () => {
    const model = createPinModel();
    model.append(1);
    model.append(2);

    model.clear();

    assert.equal(model.digits.length, 0);
    assert.equal(model.append(3).submit, false);
});

test('should ignore digits past the fourth', () => {
    const model = createPinModel();
    [1, 2, 3, 4].forEach((d) => model.append(d));

    const extra = model.append(5);

    assert.equal(extra.submit, true);
    assert.equal(extra.pin, '1234');
});
