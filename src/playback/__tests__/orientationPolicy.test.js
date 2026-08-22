import assert from 'node:assert/strict';
import { test } from 'node:test';

import { routeAllowsRotation } from '../orientationPolicy.js';

test('should allow rotation only inside the player route', () => {
    assert.equal(routeAllowsRotation(['player', '[itemId]']), true);
    assert.equal(routeAllowsRotation(['(tabs)', 'home']), false);
    assert.equal(routeAllowsRotation(['details', '[id]']), false);
    assert.equal(routeAllowsRotation([]), false);
});
