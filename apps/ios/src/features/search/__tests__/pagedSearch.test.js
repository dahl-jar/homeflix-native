import assert from 'node:assert/strict';
import { mock, test } from 'node:test';

import { createPagedSearchController } from '../pagedSearch.js';

const page = (ids, total = ids.length) => ({
    Items: ids.map((Id) => ({ Id })),
    TotalRecordCount: total
});

const deferred = () => {
    let resolve;
    let reject;
    const promise = new Promise((resolvePromise, rejectPromise) => {
        resolve = resolvePromise;
        reject = rejectPromise;
    });
    return { promise, resolve, reject };
};

const flush = async () => {
    await Promise.resolve();
    await Promise.resolve();
};

const enableTimers = (context) => {
    mock.timers.enable({ apis: ['setTimeout'] });
    context.after(() => mock.timers.reset());
};

const createHarness = ({ localResponses = [], mergedResponses = [] } = {}) => {
    const publications = [];
    const statuses = [];
    const localRequests = [];
    const mergedRequests = [];
    const controller = createPagedSearchController({
        delayMs: 1,
        pageSize: 18,
        loadLocalPage: (request) => {
            localRequests.push(request);
            return localResponses.shift() ?? Promise.resolve(page([]));
        },
        loadMergedPage: (request) => {
            mergedRequests.push(request);
            return mergedResponses.shift() ?? Promise.resolve(page([]));
        },
        onReset: () => publications.push({ items: [], source: 'reset' }),
        onResults: (result) => publications.push(result),
        onStatus: (status) => statuses.push(status)
    });
    return { controller, publications, statuses, localRequests, mergedRequests };
};

test('should publish local results while merged search is pending', async (context) => {
    enableTimers(context);
    const local = deferred();
    const merged = deferred();
    const harness = createHarness({
        localResponses: [local.promise],
        mergedResponses: [merged.promise]
    });

    harness.controller.onQuery('xmen');
    mock.timers.tick(2);
    local.resolve(page(['local-one'], 1));
    await flush();

    assert.deepEqual(harness.publications.at(-1), {
        items: [{ Id: 'local-one' }],
        total: 1,
        source: 'local'
    });
    assert.equal(harness.statuses.at(-1), 'external');
});

test('should replace the local preview with authoritative results', async (context) => {
    enableTimers(context);
    const local = deferred();
    const merged = deferred();
    const harness = createHarness({
        localResponses: [local.promise],
        mergedResponses: [merged.promise]
    });

    harness.controller.onQuery('xmen');
    mock.timers.tick(2);
    local.resolve(page(['local-one'], 1));
    await flush();
    merged.resolve(page(['merged-one', 'external-one'], 2));
    await flush();

    assert.deepEqual(harness.publications.at(-1), {
        items: [{ Id: 'merged-one' }, { Id: 'external-one' }],
        total: 2,
        source: 'merged'
    });
});

test('should ignore a local response after authoritative results', async (context) => {
    enableTimers(context);
    const local = deferred();
    const merged = deferred();
    const harness = createHarness({
        localResponses: [local.promise],
        mergedResponses: [merged.promise]
    });

    harness.controller.onQuery('xmen');
    mock.timers.tick(2);
    merged.resolve(page(['merged-one'], 1));
    await flush();
    local.resolve(page(['late-local'], 1));
    await flush();

    assert.equal(harness.publications.some((result) => result.items?.[0]?.Id === 'late-local'), false);
});

test('should append the next authoritative page', async (context) => {
    enableTimers(context);
    const first = deferred();
    const second = deferred();
    const harness = createHarness({ mergedResponses: [first.promise, second.promise] });

    harness.controller.onQuery('xmen');
    mock.timers.tick(2);
    first.resolve(page(['one'], 2));
    await flush();
    const loadMore = harness.controller.loadMore();
    second.resolve(page(['two'], 2));
    await loadMore;

    assert.deepEqual(harness.publications.at(-1).items, [{ Id: 'one' }, { Id: 'two' }]);
});

test('should ignore repeated load-more calls while a page is pending', async (context) => {
    enableTimers(context);
    const first = deferred();
    const second = deferred();
    const harness = createHarness({ mergedResponses: [first.promise, second.promise] });

    harness.controller.onQuery('xmen');
    mock.timers.tick(2);
    first.resolve(page(['one'], 3));
    await flush();
    const pending = harness.controller.loadMore();
    await harness.controller.loadMore();

    assert.equal(harness.mergedRequests.length, 2);
    second.resolve(page(['two'], 3));
    await pending;
});

test('should stop requesting pages at the total count', async (context) => {
    enableTimers(context);
    const first = deferred();
    const harness = createHarness({ mergedResponses: [first.promise] });

    harness.controller.onQuery('xmen');
    mock.timers.tick(2);
    first.resolve(page(['one'], 1));
    await flush();
    await harness.controller.loadMore();

    assert.equal(harness.mergedRequests.length, 1);
});

test('should abort both phases of the previous query', async (context) => {
    enableTimers(context);
    const harness = createHarness();

    harness.controller.onQuery('first');
    mock.timers.tick(2);
    harness.controller.onQuery('second');
    mock.timers.tick(2);

    assert.equal(harness.localRequests[0].signal.aborted, true);
    assert.equal(harness.mergedRequests[0].signal.aborted, true);
});

test('should retain local results when authoritative search fails', async (context) => {
    enableTimers(context);
    const local = deferred();
    const merged = deferred();
    const harness = createHarness({
        localResponses: [local.promise],
        mergedResponses: [merged.promise]
    });

    harness.controller.onQuery('xmen');
    mock.timers.tick(2);
    local.resolve(page(['local-one'], 1));
    await flush();
    merged.reject(new Error('unavailable'));
    await flush();

    assert.equal(harness.publications.at(-1).items[0].Id, 'local-one');
    assert.equal(harness.statuses.at(-1), 'error');
});

test('should not resume external loading when local results arrive after failure', async (context) => {
    enableTimers(context);
    const local = deferred();
    const merged = deferred();
    const harness = createHarness({
        localResponses: [local.promise],
        mergedResponses: [merged.promise]
    });

    harness.controller.onQuery('xmen');
    mock.timers.tick(2);
    merged.reject(new Error('unavailable'));
    await flush();
    local.resolve(page(['local-one'], 1));
    await flush();

    assert.equal(harness.publications.at(-1).items[0].Id, 'local-one');
    assert.equal(harness.statuses.at(-1), 'error');
});

test('should retain accumulated results when a later page fails', async (context) => {
    enableTimers(context);
    const first = deferred();
    const second = deferred();
    const harness = createHarness({
        mergedResponses: [first.promise, second.promise]
    });

    harness.controller.onQuery('xmen');
    mock.timers.tick(2);
    first.resolve(page(['one'], 2));
    await flush();
    const loadMore = harness.controller.loadMore();
    second.reject(new Error('unavailable'));
    await loadMore;

    assert.deepEqual(harness.publications.at(-1).items, [{ Id: 'one' }]);
    assert.equal(harness.statuses.at(-1), 'error');
});

test('should ignore a later page from the previous query', async (context) => {
    enableTimers(context);
    const first = deferred();
    const stalePage = deferred();
    const second = deferred();
    const harness = createHarness({
        mergedResponses: [first.promise, stalePage.promise, second.promise]
    });

    harness.controller.onQuery('first');
    mock.timers.tick(2);
    first.resolve(page(['first-one'], 2));
    await flush();
    const staleLoad = harness.controller.loadMore();
    harness.controller.onQuery('second');
    mock.timers.tick(2);
    second.resolve(page(['second-one'], 1));
    await flush();
    stalePage.resolve(page(['stale-two'], 2));
    await staleLoad;

    assert.deepEqual(harness.publications.at(-1).items, [{ Id: 'second-one' }]);
});
