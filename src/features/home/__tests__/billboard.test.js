import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';

import { billboardItems } from '../billboard.js';

const recommendations = JSON.parse(
    readFileSync(new URL('../../../api/__tests__/fixtures/recommendations.json', import.meta.url), 'utf8')
);

const resolveAll = (recs) =>
    Object.fromEntries(recs.map((rec) => [rec.ItemId, { Id: rec.ItemId, Name: `item ${rec.Rank}` }]));

test('should order billboard by rank and cap at eight', () => {
    const shuffled = [...recommendations].reverse();

    const items = billboardItems(shuffled, resolveAll(recommendations));

    assert.ok(items.length <= 8);
    assert.equal(items[0].Id, recommendations.find((rec) => rec.Rank === 1).ItemId);
    const ranksInOrder = items.map((item) => Number(item.Name.replace('item ', '')));
    assert.deepEqual(ranksInOrder, [...ranksInOrder].sort((a, b) => a - b));
});

test('should drop billboard entries whose item did not resolve', () => {
    const resolved = resolveAll(recommendations);
    delete resolved[recommendations[0].ItemId];

    const items = billboardItems(recommendations, resolved);

    assert.ok(items.every((item) => item.Id !== recommendations[0].ItemId));
});
