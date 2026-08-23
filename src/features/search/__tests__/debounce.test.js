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

test('should clear a pending search when disposed', (context) => {
    mock.timers.enable({ apis: ['setTimeout'] });
    context.after(() => mock.timers.reset());
    const runs = [];
    const controller = createSearchController({ run: async (query) => runs.push(query) });

    controller.onQuery('matrix');
    controller.dispose();
    mock.timers.tick(400);

    assert.deepEqual(runs, []);
});

test('should abort an active search when disposed', (context) => {
    mock.timers.enable({ apis: ['setTimeout'] });
    context.after(() => mock.timers.reset());
    const signals = [];
    const controller = createSearchController({
        run: (_query, signal) => new Promise(() => signals.push(signal))
    });

    controller.onQuery('matrix');
    mock.timers.tick(400);
    controller.dispose();

    assert.equal(signals[0].aborted, true);
});
