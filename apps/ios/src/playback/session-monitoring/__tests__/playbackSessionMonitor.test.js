import assert from 'node:assert/strict';
import { test } from 'node:test';

import { createPlaybackSessionMonitor } from '../playbackSessionMonitor.js';

function context() {
    return {
        itemId: 'item-one',
        mediaSourceId: 'source-one',
        playSessionId: 'session-one',
        pipelineId: 'pipeline-one',
        attemptId: 'pipeline-one-a1',
        playMethod: 'DirectPlay',
        audioStreamIndex: 1,
        subtitleStreamIndex: -1
    };
}

test('should report start progress and stop once in order', async () => {
    const calls = [];
    const client = {
        postNoContent: async (path, body) => calls.push({ path, body })
    };
    let time = 0;
    const sessionMonitor = createPlaybackSessionMonitor({ client, context: context(), now: () => time });

    await sessionMonitor.start({ positionTicks: 10, isPaused: false });
    time = 11_000;
    await sessionMonitor.progress({ positionTicks: 20, isPaused: false });
    await sessionMonitor.stop({ positionTicks: 30, isPaused: true });
    await sessionMonitor.stop({ positionTicks: 40, isPaused: true });

    assert.deepEqual(calls.map(({ path }) => path), [
        '/Sessions/Playing',
        '/Sessions/Playing/Progress',
        '/Sessions/Playing/Stopped'
    ]);
    assert.equal(calls[2].body.PositionTicks, 30);
});

test('should throttle routine progress but allow forced pause reports', async () => {
    const calls = [];
    const client = { postNoContent: async (path) => calls.push(path) };
    let time = 0;
    const sessionMonitor = createPlaybackSessionMonitor({ client, context: context(), now: () => time });

    await sessionMonitor.start({ positionTicks: 0, isPaused: false });
    time = 1_000;
    assert.equal(await sessionMonitor.progress({ positionTicks: 10, isPaused: false }), false);
    assert.equal(await sessionMonitor.progress({ positionTicks: 10, isPaused: true }, { force: true }), true);

    assert.deepEqual(calls, ['/Sessions/Playing', '/Sessions/Playing/Progress']);
});

test('should attach source track and correlation fields to reports', async () => {
    const calls = [];
    const sessionMonitor = createPlaybackSessionMonitor({
        client: { postNoContent: async (path, body) => calls.push({ path, body }) },
        context: context(),
        now: () => 0
    });

    await sessionMonitor.start({ positionTicks: 123, isPaused: false });

    assert.deepEqual(calls[0].body, {
        ItemId: 'item-one',
        MediaSourceId: 'source-one',
        PlaySessionId: 'session-one',
        PlaybackPipelineId: 'pipeline-one',
        PlaybackAttemptId: 'pipeline-one-a1',
        PlayMethod: 'DirectPlay',
        AudioStreamIndex: 1,
        SubtitleStreamIndex: -1,
        CanSeek: true,
        IsPaused: false,
        IsMuted: false,
        PositionTicks: 123
    });
});

test('should keep playback running when a monitoring request fails', async () => {
    const events = [];
    const sessionMonitor = createPlaybackSessionMonitor({
        client: { postNoContent: async () => { throw new Error('offline'); } },
        context: context(),
        now: () => 0,
        telemetry: { log: (event, fields) => events.push({ event, fields }) }
    });

    assert.equal(await sessionMonitor.start({ positionTicks: 0, isPaused: false }), false);
    assert.equal(events[0].event, 'reporting_failed');
    assert.equal(events[0].fields.reason, 'playback_start');
});
