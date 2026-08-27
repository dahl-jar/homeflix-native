import assert from 'node:assert/strict';
import { test } from 'node:test';

import { createPlaybackRuntime } from '../playbackRuntime.js';

function fakePlayer() {
    const listeners = new Map();
    return {
        audioTrack: null,
        subtitleTrack: null,
        currentTime: 0,
        duration: 100,
        playing: false,
        addListener(name, listener) {
            listeners.set(name, listener);
            return { remove: () => listeners.delete(name) };
        },
        replaceAsync: async function replaceAsync(source) {
            this.source = source;
        },
        play() {
            this.playing = true;
        },
        pause() {
            this.playing = false;
        },
        seekBy(seconds) {
            this.currentTime += seconds;
        },
        listeners
    };
}

function acceptedSession(events, mediaSourceId = 'source-one') {
    return {
        context: {
            mediaSourceId,
            audioStreamIndex: 3,
            subtitleStreamIndex: -1
        },
        pipeline: { pipelineId: 'pipeline-one' },
        telemetry: { log: (event, fields) => events.push({ owner: 'telemetry', event, fields }) },
        sessionMonitor: {
            start: async (snapshot) => events.push({ owner: 'sessionMonitor', event: 'start', snapshot }),
            progress: async (snapshot, options) => events.push({ owner: 'sessionMonitor', event: 'progress', snapshot, options }),
            stop: async (snapshot) => events.push({ owner: 'sessionMonitor', event: 'stop', snapshot })
        },
        attemptedSourceIds: [mediaSourceId],
        startSeconds: 5,
        video: { source: { uri: `http://server/${mediaSourceId}.m3u8`, contentType: 'hls' } }
    };
}

function serverTrackMetadata(audioStreamIndex = 3, subtitleStreamIndex = -1) {
    const audioTracks = [
        { label: 'Japanese FLAC', streamIndex: 1, serverResolved: true },
        { label: 'English AAC', streamIndex: 3, serverResolved: true }
    ];
    const subtitleTracks = [
        { label: 'English ASS', streamIndex: 2, serverResolved: true }
    ];
    return {
        audioTracks,
        subtitleTracks,
        selectedAudioTrack: audioTracks.find((track) => track.streamIndex === audioStreamIndex),
        selectedSubtitleTrack: subtitleTracks.find((track) =>
            track.streamIndex === subtitleStreamIndex
        ) ?? null
    };
}

function waitFor(promise) {
    return Promise.race([
        promise,
        new Promise((resolve, reject) => setTimeout(() => reject(new Error('override not requested')), 100))
    ]);
}

test('should connect native events to session monitoring and terminal cleanup', async () => {
    const events = [];
    const snapshots = [];
    const player = fakePlayer();
    const runtime = createPlaybackRuntime({
        negotiationOptions: {},
        negotiate: async () => acceptedSession(events),
        onSnapshot: (snapshot) => snapshots.push(snapshot),
        player
    });

    await runtime.start();
    player.listeners.get('statusChange')({ status: 'readyToPlay' });
    player.listeners.get('playingChange')({ isPlaying: true });
    player.currentTime = 8;
    player.listeners.get('timeUpdate')({ currentTime: 8, bufferedPosition: 20 });
    await runtime.stop();

    assert.equal(player.source.contentType, 'hls');
    assert.equal(player.currentTime, 8);
    assert.equal(events.filter(({ owner }) => owner === 'sessionMonitor').map(({ event }) => event).join(','), 'start,progress,stop');
    assert.equal(snapshots.at(-1).status, 'ended');
    assert.equal(player.listeners.size, 0);
});

test('should expose native player handoff in runtime snapshots', async () => {
    const snapshots = [];
    const player = fakePlayer();
    const runtime = createPlaybackRuntime({
        negotiationOptions: {},
        async negotiate(options) {
            options.onPipelineProgress({ type: 'resolution_started' });
            options.onPipelineProgress({ type: 'resolution_completed' });
            options.onPipelineProgress({ type: 'release_started' });
            options.onPipelineProgress({ type: 'release_completed' });
            return acceptedSession([]);
        },
        onSnapshot: (snapshot) => snapshots.push(snapshot),
        player
    });

    await runtime.start();

    assert.equal(snapshots.at(-1).pipeline.videoVisible, true);
    assert.equal(snapshots.at(-1).pipeline.visible, true);
    assert.equal(snapshots.at(-1).pipeline.stages.at(-1).status, 'active');

    player.listeners.get('playingChange')({ isPlaying: true });

    assert.equal(snapshots.at(-1).pipeline.visible, false);
    assert.equal(snapshots.at(-1).pipeline.stages.at(-1).status, 'complete');
    await runtime.stop();
});

test('should keep advancing HLS when video-track metadata is absent', async () => {
    const events = [];
    const player = fakePlayer();
    const negotiationCalls = [];
    const runtime = createPlaybackRuntime({
        negotiationOptions: {},
        async negotiate(options) {
            negotiationCalls.push(options);
            return acceptedSession(events, `source-${negotiationCalls.length}`);
        },
        onSnapshot: () => {},
        player
    });

    await runtime.start();
    player.listeners.get('sourceLoad')({
        duration: 100,
        availableAudioTracks: [],
        availableSubtitleTracks: [],
        availableVideoTracks: []
    });
    player.listeners.get('playingChange')({ isPlaying: true });
    player.currentTime = 7;
    player.listeners.get('timeUpdate')({ currentTime: 7, bufferedPosition: 20 });
    await new Promise((resolve) => setTimeout(resolve, 0));

    assert.equal(negotiationCalls.length, 1);
    assert.equal(events.filter(({ owner, event }) => owner === 'sessionMonitor' && event === 'start').length, 1);
    await runtime.stop();
});

test('should expose player commands without policy', async () => {
    const player = fakePlayer();
    const runtime = createPlaybackRuntime({
        negotiationOptions: {},
        negotiate: async () => acceptedSession([]),
        onSnapshot: () => {},
        player
    });

    await runtime.start();
    runtime.pause();
    assert.equal(player.playing, false);
    runtime.play();
    assert.equal(player.playing, true);
    runtime.seekBy(15);
    assert.equal(player.currentTime, 20);
    runtime.seekTo(4);
    assert.equal(player.currentTime, 4);
    const audioTrack = { language: 'ja', label: 'Japanese' };
    const subtitleTrack = { language: 'en', label: 'English' };
    runtime.selectAudioTrack(audioTrack);
    runtime.selectSubtitleTrack(subtitleTrack);
    assert.equal(player.audioTrack, audioTrack);
    assert.equal(player.subtitleTrack, subtitleTrack);
    await runtime.stop();
});

test('should complete player startup when media time advances without playing change', async () => {
    const events = [];
    const player = fakePlayer();
    const runtime = createPlaybackRuntime({
        negotiationOptions: {},
        negotiate: async () => acceptedSession(events),
        onSnapshot: () => {},
        player
    });

    await runtime.start();
    player.listeners.get('timeUpdate')({ currentTime: 6, bufferedPosition: 20 });

    assert.equal(runtime.getSnapshot().status, 'playing');
    assert.equal(runtime.getSnapshot().pipeline.visible, false);
    assert.equal(events.filter(({ event }) => event === 'start').length, 1);
    assert.equal(events.filter(({ event }) => event === 'playback_started').length, 1);

    player.listeners.get('playingChange')({ isPlaying: true });

    assert.equal(events.filter(({ event }) => event === 'start').length, 1);
    assert.equal(events.filter(({ event }) => event === 'playback_started').length, 1);
    await runtime.stop();
});

test('should reject a native player failure before requesting another server attempt', async () => {
    const events = [];
    const snapshots = [];
    const recoverySequence = [];
    const player = fakePlayer();
    const negotiationCalls = [];
    let signalRecovery;
    const recoveryStarted = new Promise((resolve) => {
        signalRecovery = resolve;
    });
    const runtime = createPlaybackRuntime({
        negotiationOptions: { startTimeTicks: 0 },
        async negotiate(options) {
            negotiationCalls.push(options);
            if (negotiationCalls.length === 2) {
                recoverySequence.push('negotiate');
                signalRecovery();
            }
            const accepted = acceptedSession(
                events,
                negotiationCalls.length === 1 ? 'source-one' : 'source-two'
            );
            const stop = accepted.sessionMonitor.stop;
            accepted.sessionMonitor.stop = async (snapshot) => {
                recoverySequence.push('stop');
                await stop(snapshot);
            };
            accepted.startSeconds = options.startTimeTicks / 10_000_000;
            return accepted;
        },
        onSnapshot: (snapshot) => snapshots.push(snapshot),
        player
    });

    await runtime.start();
    player.currentTime = 12;
    player.listeners.get('statusChange')({ status: 'error', error: new Error('decode failed') });
    await recoveryStarted;
    await new Promise((resolve) => setTimeout(resolve, 0));

    assert.equal(negotiationCalls.length, 2);
    assert.equal(negotiationCalls[1].excludedSourceIds.has('source-one'), true);
    assert.equal(player.source.uri, 'http://server/source-two.m3u8');
    const failedIndex = snapshots.findIndex((snapshot) =>
        snapshot.pipeline.stages.some(({ status }) => status === 'failed')
    );
    const retryIndex = snapshots.findIndex((snapshot, index) =>
        index > failedIndex && snapshot.pipeline.attempt === 2
    );
    assert.notEqual(failedIndex, -1);
    assert.ok(retryIndex > failedIndex);
    assert.deepEqual(recoverySequence, ['stop', 'negotiate']);
    await runtime.stop();
});

test('should continue to source three when source two fails during recovery', async () => {
    const events = [];
    const player = fakePlayer();
    const sources = Array.from({ length: 10 }, (_, index) => `source-${index + 1}`);
    const negotiationCalls = [];
    let resolveSourceThree;
    const sourceThreeLoaded = new Promise((resolve) => {
        resolveSourceThree = resolve;
    });
    player.replaceAsync = async function replaceAsync(source) {
        this.source = source;
        if (source.uri.endsWith('/source-2.m3u8')) {
            this.listeners.get('statusChange')({
                status: 'error',
                error: new Error('source two failed')
            });
        }
        if (source.uri.endsWith('/source-3.m3u8')) resolveSourceThree();
    };
    const runtime = createPlaybackRuntime({
        negotiationOptions: { startTimeTicks: 0 },
        async negotiate(options) {
            negotiationCalls.push(options);
            return acceptedSession(events, sources[negotiationCalls.length - 1]);
        },
        onSnapshot: () => {},
        player
    });

    await runtime.start();
    player.listeners.get('statusChange')({
        status: 'error',
        error: new Error('source one failed')
    });
    await waitFor(sourceThreeLoaded);

    assert.equal(negotiationCalls.length, 3);
    assert.deepEqual(
        [...negotiationCalls[2].excludedSourceIds],
        ['source-1', 'source-2']
    );
    assert.equal(player.source.uri, 'http://server/source-3.m3u8');
    assert.equal(negotiationCalls.some((_, index) => index > 2), false);
    await runtime.stop();
});

test('should ignore a source resolved after runtime stop', async () => {
    const player = fakePlayer();
    let resolveNegotiation;
    const negotiation = new Promise((resolve) => {
        resolveNegotiation = resolve;
    });
    const runtime = createPlaybackRuntime({
        negotiationOptions: { startTimeTicks: 0 },
        negotiate: () => negotiation,
        onSnapshot: () => {},
        player
    });

    const start = runtime.start();
    await Promise.resolve();
    await runtime.stop();
    resolveNegotiation(acceptedSession([]));
    await start;

    assert.equal(player.source, undefined);
    assert.equal(player.playing, false);
});

test('should restart at the current position for server audio and subtitle overrides', async () => {
    const events = [];
    const player = fakePlayer();
    const negotiationCalls = [];
    let signalAudioOverride;
    let signalSubtitleOverride;
    const audioOverrideRequested = new Promise((resolve) => {
        signalAudioOverride = resolve;
    });
    const subtitleOverrideRequested = new Promise((resolve) => {
        signalSubtitleOverride = resolve;
    });
    const runtime = createPlaybackRuntime({
        negotiationOptions: { startTimeTicks: 0 },
        async negotiate(options) {
            negotiationCalls.push(options);
            if (negotiationCalls.length === 2) signalAudioOverride();
            if (negotiationCalls.length === 3) signalSubtitleOverride();
            const accepted = acceptedSession(events);
            const override = {
                audioStreamIndex: 3,
                subtitleStreamIndex: -1,
                ...options.trackOverride
            };
            accepted.context.audioStreamIndex = override.audioStreamIndex;
            accepted.context.subtitleStreamIndex = override.subtitleStreamIndex;
            accepted.startSeconds = options.startTimeTicks / 10_000_000;
            accepted.trackMetadata = serverTrackMetadata(
                override.audioStreamIndex,
                override.subtitleStreamIndex
            );
            return accepted;
        },
        onSnapshot: () => {},
        player
    });

    await runtime.start();
    player.currentTime = 12;
    const japanese = runtime.getSnapshot().audioTracks.find(({ streamIndex }) => streamIndex === 1);
    runtime.selectAudioTrack(japanese);
    await waitFor(audioOverrideRequested);
    await new Promise((resolve) => setTimeout(resolve, 0));

    assert.deepEqual(negotiationCalls[1].trackOverride, {
        mediaSourceId: 'source-one',
        audioStreamIndex: 1,
        subtitleStreamIndex: -1
    });
    assert.equal(negotiationCalls[1].startTimeTicks, 120_000_000);

    player.currentTime = 14;
    const englishSubtitle = runtime.getSnapshot().subtitleTracks[0];
    runtime.selectSubtitleTrack(englishSubtitle);
    await waitFor(subtitleOverrideRequested);
    await new Promise((resolve) => setTimeout(resolve, 0));

    assert.deepEqual(negotiationCalls[2].trackOverride, {
        mediaSourceId: 'source-one',
        audioStreamIndex: 1,
        subtitleStreamIndex: 2
    });
    assert.equal(negotiationCalls[2].startTimeTicks, 140_000_000);
    assert.equal(runtime.getSnapshot().selectedAudioTrack.label, 'Japanese FLAC');
    assert.equal(runtime.getSnapshot().selectedSubtitleTrack.label, 'English ASS');
    await runtime.stop();
});
