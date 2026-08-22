import assert from 'node:assert/strict';
import { test } from 'node:test';

import { createPager } from '../paging.js';

const page = (startIndex, count, total) => ({
    Items: Array.from({ length: count }, (_, i) => ({ Id: `item-${startIndex + i}` })),
    TotalRecordCount: total
});

test('should request next page within twenty items of the end', () => {
    const pager = createPager({ pageSize: 100 });
    pager.applyPage(page(0, 100, 300));

    assert.equal(pager.shouldLoadMore(50), false);
    assert.equal(pager.shouldLoadMore(80), true);
});

test('should not page past total count', () => {
    const pager = createPager({ pageSize: 100 });
    pager.applyPage(page(0, 100, 100));

    assert.equal(pager.shouldLoadMore(99), false);
});

test('should track next start index across applied pages', () => {
    const pager = createPager({ pageSize: 100 });
    pager.applyPage(page(0, 100, 250));
    pager.applyPage(page(100, 100, 250));

    assert.equal(pager.items.length, 200);
    assert.equal(pager.nextStartIndex(), 200);
});
