import assert from 'node:assert/strict';
import { test, mock } from 'node:test';

import { createSearchController } from '../debounce.js';

test('should ignore whitespace-only search input', () => {
    mock.timers.enable({ apis: ['setTimeout'] });
    const runs = [];
    const controller = createSearchController({ delayMs: 350, run: async (q) => runs.push(q) });

    controller.onQuery('   ');
    mock.timers.tick(400);

    assert.equal(runs.length, 0);
    mock.timers.reset();
});

test('should run the trimmed query after the delay', () => {
    mock.timers.enable({ apis: ['setTimeout'] });
    const runs = [];
    const controller = createSearchController({ delayMs: 350, run: async (q) => runs.push(q) });

    controller.onQuery('  matrix ');
    mock.timers.tick(300);
    assert.equal(runs.length, 0);
    mock.timers.tick(100);

    assert.deepEqual(runs, ['matrix']);
    mock.timers.reset();
});

test('should cancel in-flight search when query changes', () => {
    mock.timers.enable({ apis: ['setTimeout'] });
    const signals = [];
    const controller = createSearchController({
        delayMs: 350,
        run: (q, signal) => new Promise(() => signals.push({ q, signal }))
    });

    controller.onQuery('first');
    mock.timers.tick(400);
    controller.onQuery('second');
    mock.timers.tick(400);

    assert.equal(signals[0].signal.aborted, true);
    assert.equal(signals[1].signal.aborted, false);
    mock.timers.reset();
});
