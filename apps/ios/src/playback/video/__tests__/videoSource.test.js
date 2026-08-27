import assert from 'node:assert/strict';
import { test } from 'node:test';

import { createVideoSource } from '../videoSource.js';

const base = {
    serverUrl: 'http://server',
    itemId: 'item-one',
    playbackInfo: { PlaySessionId: 'session-one' },
    mediaSource: { Id: 'source-one', Container: 'mp4', Path: 'https://provider/secret' },
    tracks: { audioStreamIndex: 1, subtitleStreamIndex: -1 },
    mediaHeaders: { Authorization: 'MediaBrowser Token="secret"' },
    pipelineId: 'pipeline-one',
    attemptId: 'pipeline-one-a1'
};

test('should build an authenticated server stream for direct play', () => {
    const result = createVideoSource({ ...base, playMethod: 'DirectPlay' });

    const url = new URL(result.source.uri);
    assert.equal(url.pathname, '/Videos/item-one/stream');
    assert.equal(url.searchParams.get('Static'), 'true');
    assert.equal(url.searchParams.get('MediaSourceId'), 'source-one');
    assert.equal(url.searchParams.get('PlaySessionId'), 'session-one');
    assert.equal(url.searchParams.get('PlaybackPipelineId'), 'pipeline-one');
    assert.deepEqual(result.source.headers, base.mediaHeaders);
    assert.equal(result.playMethod, 'DirectPlay');
    assert.doesNotMatch(result.source.uri, /provider|secret|Token/);
});

test('should use the released transcoding path for HLS', () => {
    const result = createVideoSource({
        ...base,
        playMethod: 'Transcode',
        mediaSource: {
            Id: 'source-one',
            Container: 'mkv',
            TranscodingUrl: '/videos/item-one/master.m3u8?MediaSourceId=source-one'
        }
    });

    assert.equal(result.source.uri, 'http://server/videos/item-one/master.m3u8?MediaSourceId=source-one');
    assert.equal(result.source.contentType, 'hls');
    assert.equal(result.playMethod, 'Transcode');
});

test('should reject a transcoding source without a released url', () => {
    assert.throws(
        () => createVideoSource({ ...base, playMethod: 'Transcode' }),
        /transcoding url/
    );
});
