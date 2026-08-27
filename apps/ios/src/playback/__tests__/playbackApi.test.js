import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
    resolvePlaybackAttempt,
    releasePlaybackSource,
    reportPlaybackStart,
    reportPlaybackProgress,
    reportPlaybackStop
} from '../playbackApi.js';
import { getPlaybackProgress } from '../playbackProgressApi.js';

function recordingClient(result = {}) {
    const calls = [];
    return {
        calls,
        post(path, body, params) {
            calls.push({ method: 'post', path, body, params });
            return Promise.resolve(result);
        },
        postNoContent(path, body, params) {
            calls.push({ method: 'postNoContent', path, body, params });
            return Promise.resolve();
        }
    };
}

const baseRequest = {
    itemId: 'item-one',
    userId: 'user-one',
    deviceProfile: { Name: 'Homeflix iOS' },
    startTimeTicks: 120_000_000
};

test('should ask the server to resolve one correlated playback attempt', async () => {
    const response = { MediaSources: [{ Id: 'source-one' }] };
    const client = recordingClient(response);

    const result = await resolvePlaybackAttempt(client, {
        ...baseRequest,
        pipelineId: 'pipeline-one',
        attemptId: 'pipeline-one-a1',
        rejectedSourceIds: new Set(['source-bad']),
        preferredMediaSourceId: 'source-preferred'
    });

    assert.equal(result, response);
    assert.deepEqual(client.calls[0], {
        method: 'post',
        path: '/Items/item-one/PlaybackInfo',
        body: {
            UserId: 'user-one',
            DeviceProfile: { Name: 'Homeflix iOS' },
            StartTimeTicks: 120_000_000,
            EnableDirectPlay: true,
            EnableDirectStream: true,
            EnableTranscoding: true,
            AllowVideoStreamCopy: true,
            AllowAudioStreamCopy: true,
            PlaybackPipelineId: 'pipeline-one',
            PlaybackAttemptId: 'pipeline-one-a1',
            PlaybackPipelineResolve: true,
            PlaybackRejectedSourceIds: ['source-bad'],
            PlaybackPreferredMediaSourceId: 'source-preferred'
        },
        params: undefined
    });
});

test('should request retained progress after the consumed sequence', async () => {
    const response = { Events: [{ Sequence: 4, StageId: 'analysis' }] };
    const client = recordingClient(response);
    client.get = (path, params) => {
        client.calls.push({ method: 'get', path, params });
        return Promise.resolve(response);
    };

    const result = await getPlaybackProgress(client, {
        pipelineId: 'pipeline-one',
        attemptId: 'pipeline-one-a1',
        afterSequence: 3
    });

    assert.equal(result, response);
    assert.deepEqual(client.calls[0], {
        method: 'get',
        path: '/Playback/PipelineProgress',
        params: {
            pipelineId: 'pipeline-one',
            attemptId: 'pipeline-one-a1',
            afterSequence: 3
        }
    });
});

test('should forward a forced HLS request policy to the resolver', async () => {
    const client = recordingClient({ MediaSources: [] });

    await resolvePlaybackAttempt(client, {
        ...baseRequest,
        pipelineId: 'pipeline-one',
        attemptId: 'pipeline-one-a1',
        enableDirectPlay: false,
        enableDirectStream: false,
        allowVideoStreamCopy: false,
        allowAudioStreamCopy: false
    });

    assert.equal(client.calls[0].body.EnableDirectPlay, false);
    assert.equal(client.calls[0].body.EnableDirectStream, false);
    assert.equal(client.calls[0].body.EnableTranscoding, true);
    assert.equal(client.calls[0].body.AllowVideoStreamCopy, false);
    assert.equal(client.calls[0].body.AllowAudioStreamCopy, false);
});

test('should send exact audio and subtitle overrides to the resolver', async () => {
    const client = recordingClient({ MediaSources: [] });

    await resolvePlaybackAttempt(client, {
        ...baseRequest,
        pipelineId: 'pipeline-one',
        attemptId: 'pipeline-one-a2',
        preferredMediaSourceId: 'source-one',
        trackOverride: {
            audioStreamIndex: 1,
            subtitleStreamIndex: 2
        }
    });

    assert.equal(client.calls[0].body.PlaybackPipelineTrackOverride, true);
    assert.equal(client.calls[0].body.AudioStreamIndex, 1);
    assert.equal(client.calls[0].body.SubtitleStreamIndex, 2);
    assert.equal(client.calls[0].body.PlaybackPreferredMediaSourceId, 'source-one');
});

test('should exchange an accepted preflight for one released source', async () => {
    const client = recordingClient({ MediaSources: [{ Id: 'source-one' }] });

    await releasePlaybackSource(client, {
        ...baseRequest,
        mediaSourceId: 'source-one',
        pipelineId: 'pipeline-one',
        attemptId: 'pipeline-one-a1',
        pipelineHandle: 'signed-handle',
        pipelineDecision: 'no_subtitle_needed',
        audioStreamIndex: 1,
        subtitleStreamIndex: -1
    });

    assert.deepEqual(client.calls[0].body, {
        UserId: 'user-one',
        DeviceProfile: { Name: 'Homeflix iOS' },
        StartTimeTicks: 120_000_000,
        EnableDirectPlay: true,
        EnableDirectStream: true,
        EnableTranscoding: true,
        AllowVideoStreamCopy: true,
        AllowAudioStreamCopy: true,
        MediaSourceId: 'source-one',
        PlaybackPipelineId: 'pipeline-one',
        PlaybackAttemptId: 'pipeline-one-a1',
        PlaybackPipelineHandle: 'signed-handle',
        PlaybackPipelineAccepted: true,
        PlaybackPipelineDecision: 'no_subtitle_needed',
        AudioStreamIndex: 1,
        SubtitleStreamIndex: -1
    });
});

test('should report start progress and stop to Jellyfin session endpoints', async () => {
    const client = recordingClient();
    const payload = {
        ItemId: 'item-one',
        MediaSourceId: 'source-one',
        PlaySessionId: 'session-one',
        PlaybackPipelineId: 'pipeline-one',
        PlaybackAttemptId: 'pipeline-one-a1'
    };

    await reportPlaybackStart(client, payload);
    await reportPlaybackProgress(client, payload);
    await reportPlaybackStop(client, payload);

    assert.deepEqual(client.calls.map(({ method, path }) => ({ method, path })), [
        { method: 'postNoContent', path: '/Sessions/Playing' },
        { method: 'postNoContent', path: '/Sessions/Playing/Progress' },
        { method: 'postNoContent', path: '/Sessions/Playing/Stopped' }
    ]);
});
