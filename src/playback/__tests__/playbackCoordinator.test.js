import assert from 'node:assert/strict';
import { test } from 'node:test';

import { negotiatePlayback } from '../playbackCoordinator.js';

function source(fields = {}) {
    return {
        Id: 'source-two',
        SupportsDirectPlay: false,
        SupportsTranscoding: true,
        MediaStreams: [
            { Type: 'Audio', Index: 4, Language: 'eng', DisplayTitle: 'English AAC' },
            { Type: 'Subtitle', Index: 7, Language: 'eng', DisplayTitle: 'English ASS' }
        ],
        ...fields
    };
}

function clientFor(response) {
    const calls = [];
    return {
        calls,
        mediaHeaders: { Authorization: 'MediaBrowser Token="secret"' },
        async get(path, params) {
            calls.push({ path, params });
            return { Events: [] };
        },
        async post(path, body) {
            calls.push({ path, body });
            return typeof response === 'function' ? response(body) : response;
        },
        async postNoContent(path, body) {
            calls.push({ path, body });
        }
    };
}

function resolved(sourceFields = {}) {
    return {
        PlaybackPipelineHandle: 'signed-handle',
        PlaybackPipelineDecision: 'subtitle_language_matched',
        PlaybackPipelineAudioStreamIndex: 4,
        PlaybackPipelineSubtitleStreamIndex: 7,
        PlaybackPipelineSourceCount: 3,
        MediaSources: [source(sourceFields)]
    };
}

function options(client, fields = {}) {
    return {
        client,
        item: { Id: 'item-one', Name: 'Movie' },
        platform: 'ios',
        serverUrl: 'http://server',
        startTimeTicks: 20_000_000,
        userId: 'user-one',
        createId: () => 'pipeline-one',
        ...fields
    };
}

test('should release exactly the source and tracks resolved by the server', async () => {
    const progressEvents = [];
    const client = clientFor((body) => body.PlaybackPipelineResolve
        ? resolved()
        : {
            PlaySessionId: 'session-one',
            MediaSources: [source({ TranscodingUrl: '/videos/item/master.m3u8' })]
        });

    const result = await negotiatePlayback(options(client, {
        onPipelineProgress: (event) => progressEvents.push(event.type)
    }));

    assert.equal(result.context.mediaSourceId, 'source-two');
    assert.equal(result.context.attemptId, 'pipeline-one-a1');
    assert.equal(result.context.audioStreamIndex, 4);
    assert.equal(result.context.subtitleStreamIndex, 7);
    assert.equal(result.context.playSessionId, 'session-one');
    assert.equal(result.context.playMethod, 'Transcode');
    assert.equal(result.video.source.contentType, 'hls');
    assert.equal(result.startSeconds, 2);
    assert.deepEqual(result.attemptedSourceIds, ['source-two']);
    assert.equal(result.trackMetadata.audioTracks[0].label, 'English');
    assert.equal(result.trackMetadata.selectedAudioTrack.label, 'English');
    assert.equal(result.trackMetadata.selectedSubtitleTrack.label, 'English');
    const playbackCalls = client.calls.filter(({ path }) => path === '/Items/item-one/PlaybackInfo');
    assert.equal(playbackCalls.length, 2);
    const release = playbackCalls[1].body;
    assert.equal(release.PlaybackPipelineDecision, 'subtitle_language_matched');
    assert.equal(release.AudioStreamIndex, 4);
    assert.equal(release.SubtitleStreamIndex, 7);
    assert.equal(release.PlaybackPipelineHandle, 'signed-handle');
    assert.equal(release.EnableDirectPlay, false);
    assert.equal(release.EnableDirectStream, false);
    assert.equal(release.AllowVideoStreamCopy, false);
    assert.equal(release.AllowAudioStreamCopy, false);
    assert.doesNotMatch(JSON.stringify(result.context), /secret|videos|m3u8/);
    assert.deepEqual(progressEvents, [
        'resolution_started',
        'resolution_completed',
        'stage_progress',
        'stage_progress',
        'release_completed'
    ]);
});

test('should forward player-rejected and preferred sources to the server', async () => {
    const client = clientFor((body) => body.PlaybackPipelineResolve
        ? resolved({ SupportsDirectPlay: true, SupportsTranscoding: false })
        : { PlaySessionId: 'session-one', MediaSources: [source({ SupportsDirectPlay: true })] });

    await negotiatePlayback(options(client, {
        excludedSourceIds: new Set(['source-one']),
        preferredMediaSourceId: 'source-two'
    }));

    const request = client.calls.find(({ body }) => body?.PlaybackPipelineResolve).body;
    assert.deepEqual(request.PlaybackRejectedSourceIds, ['source-one']);
    assert.equal(request.PlaybackPreferredMediaSourceId, 'source-two');
});

test('should drain real server stages before signed release', async () => {
    const progressEvents = [];
    const client = clientFor((body) => body.PlaybackPipelineResolve
        ? resolved()
        : {
            PlaySessionId: 'session-one',
            MediaSources: [source({ TranscodingUrl: '/videos/item/master.m3u8' })]
        });
    const serverEvent = (status, sourceAttempt) => ({
        type: 'stage_progress',
        sequence: sourceAttempt,
        stageId: 'analysis',
        label: 'Analyzing source',
        order: 20,
        status,
        sourceAttempt
    });

    await negotiatePlayback(options(client, {
        onPipelineProgress: (event) => progressEvents.push(event),
        watchProgress({ onProgress }) {
            onProgress(serverEvent('active', 1));
            return {
                async stop() {
                    onProgress(serverEvent('failed', 1));
                    onProgress(serverEvent('active', 2));
                }
            };
        }
    }));

    assert.deepEqual(progressEvents.map(({ type, status, sourceAttempt }) => ({
        type,
        status,
        sourceAttempt
    })), [
        { type: 'resolution_started', status: undefined, sourceAttempt: undefined },
        { type: 'stage_progress', status: 'active', sourceAttempt: 1 },
        { type: 'stage_progress', status: 'failed', sourceAttempt: 1 },
        { type: 'stage_progress', status: 'active', sourceAttempt: 2 },
        { type: 'resolution_completed', status: undefined, sourceAttempt: undefined },
        { type: 'stage_progress', status: 'active', sourceAttempt: undefined },
        { type: 'stage_progress', status: 'complete', sourceAttempt: undefined },
        { type: 'release_completed', status: undefined, sourceAttempt: undefined }
    ]);
});

test('should fail when the server cannot resolve a compatible source', async () => {
    const progressEvents = [];
    const client = clientFor({ ErrorCode: 'NoCompatibleStream', MediaSources: [] });

    await assert.rejects(
        negotiatePlayback(options(client, {
            onPipelineProgress: (event) => progressEvents.push(event.type)
        })),
        /no compatible playback source/
    );
    assert.equal(
        client.calls.filter(({ path }) => path === '/Items/item-one/PlaybackInfo').length,
        1
    );
    assert.deepEqual(progressEvents, ['resolution_started']);
});

test('should reject an incomplete server resolution before release', async () => {
    const client = clientFor({
        ...resolved(),
        PlaybackPipelineAudioStreamIndex: null
    });

    await assert.rejects(
        negotiatePlayback(options(client)),
        /incomplete playback resolution/
    );
    assert.equal(
        client.calls.filter(({ path }) => path === '/Items/item-one/PlaybackInfo').length,
        1
    );
});

test('should request a manual override on the same source', async () => {
    const client = clientFor((body) => body.PlaybackPipelineResolve
        ? {
            ...resolved(),
            PlaybackPipelineDecision: 'user_track_override',
            PlaybackPipelineAudioStreamIndex: 1,
            PlaybackPipelineSubtitleStreamIndex: 7
        }
        : { PlaySessionId: 'session-two', MediaSources: [source({ SupportsDirectPlay: true })] });

    const result = await negotiatePlayback(options(client, {
        trackOverride: {
            mediaSourceId: 'source-two',
            audioStreamIndex: 1,
            subtitleStreamIndex: 7
        }
    }));

    const request = client.calls.find(({ body }) => body?.PlaybackPipelineResolve).body;
    assert.equal(request.PlaybackPipelineTrackOverride, true);
    assert.equal(request.PlaybackPreferredMediaSourceId, 'source-two');
    assert.equal(result.context.audioStreamIndex, 1);
    assert.equal(result.context.subtitleStreamIndex, 7);
});
