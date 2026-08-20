import { test } from 'node:test';
import assert from 'node:assert/strict';

import { primaryUrl, backdropUrl } from '../imageUrl.js';

const item = {
    Id: 'item-one',
    ImageTags: { Primary: 'tag-primary' },
    BackdropImageTags: ['tag-backdrop']
};

test('should build tagged primary image urls', () => {
    const url = primaryUrl('http://server', item, 400);

    assert.equal(
        url,
        'http://server/Items/item-one/Images/Primary?tag=tag-primary&maxWidth=400&quality=90'
    );
});

test('should return null when the item has no primary image', () => {
    assert.equal(primaryUrl('http://server', { Id: 'item-two', ImageTags: {} }, 400), null);
});

test('should fall back to the series poster for episodes without own art', () => {
    const episode = {
        Id: 'episode-one',
        ImageTags: {},
        SeriesId: 'series-one',
        SeriesPrimaryImageTag: 'tag-series'
    };

    assert.equal(
        primaryUrl('http://server', episode, 300),
        'http://server/Items/series-one/Images/Primary?tag=tag-series&maxWidth=300&quality=90'
    );
});

test('should build backdrop urls from the first backdrop tag', () => {
    const url = backdropUrl('http://server', item, 1280);

    assert.equal(
        url,
        'http://server/Items/item-one/Images/Backdrop/0?tag=tag-backdrop&maxWidth=1280&quality=90'
    );
});
