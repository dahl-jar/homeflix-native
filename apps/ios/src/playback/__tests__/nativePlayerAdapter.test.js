import assert from 'node:assert/strict';
import { test } from 'node:test';

import { bindNativePlayer } from '../nativePlayerAdapter.js';

function fakePlayer() {
    const listeners = new Map();
    return {
        audioTrack: null,
        subtitleTrack: null,
        currentTime: 0,
        duration: 90,
        playing: false,
        timeUpdateEventInterval: 0,
        listeners,
        addListener(name, listener) {
            listeners.set(name, listener);
            return { remove: () => listeners.delete(name) };
        },
        async replaceAsync(source) {
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
        }
    };
}

test('should load asynchronously seek and begin playback', async () => {
    const player = fakePlayer();
    const binding = bindNativePlayer(player, {});

    await binding.load({ uri: 'http://server/video.m3u8', contentType: 'hls' }, 12);

    assert.equal(player.source.contentType, 'hls');
    assert.equal(player.currentTime, 12);
    assert.equal(player.playing, true);
    assert.equal(player.timeUpdateEventInterval, 1);
    binding.dispose();
    assert.equal(player.playing, false);
    assert.equal(player.listeners.size, 0);
});

test('should translate player events into lifecycle callbacks', () => {
    const player = fakePlayer();
    const events = [];
    const binding = bindNativePlayer(player, {
        onEnded: () => events.push({ type: 'ended' }),
        onPlayingChange: (isPlaying) => events.push({ type: 'playing', isPlaying }),
        onReady: (payload) => events.push({ type: 'ready', ...payload }),
        onTimeUpdate: (payload) => events.push({ type: 'time', ...payload })
    });

    player.listeners.get('statusChange')({ status: 'readyToPlay' });
    player.listeners.get('playingChange')({ isPlaying: true });
    player.listeners.get('timeUpdate')({ currentTime: 8, bufferedPosition: 20 });
    player.listeners.get('playToEnd')();

    assert.deepEqual(events, [
        { type: 'ready', durationSeconds: 90 },
        { type: 'playing', isPlaying: true },
        {
            type: 'time',
            positionSeconds: 8,
            durationSeconds: 90,
            bufferedSeconds: 20,
            playbackAdvanced: true
        },
        { type: 'ended' }
    ]);
    binding.dispose();
});

test('should not play when replacement finishes after disposal', async () => {
    const player = fakePlayer();
    let finishReplacement;
    player.replaceAsync = () => new Promise((resolve) => {
        finishReplacement = resolve;
    });
    const binding = bindNativePlayer(player, {});

    const loading = binding.load({ uri: 'http://server/video.m3u8', contentType: 'hls' });
    binding.dispose();
    finishReplacement();
    await loading;

    assert.equal(player.playing, false);
});

test('should absorb pause rejection after the native player is released', async () => {
    const player = fakePlayer();
    let pauseCalls = 0;
    player.pause = () => {
        pauseCalls += 1;
        return Promise.reject(new Error('native player released'));
    };
    const binding = bindNativePlayer(player, {});

    binding.dispose();
    await new Promise((resolve) => setTimeout(resolve, 0));

    assert.equal(pauseCalls, 1);
    assert.equal(player.listeners.size, 0);
});

test('should report the last observed snapshot after the native player is released', () => {
    const player = fakePlayer();
    const binding = bindNativePlayer(player, {});
    player.listeners.get('playingChange')({ isPlaying: true });
    player.listeners.get('timeUpdate')({ currentTime: 18, bufferedPosition: 24 });
    for (const property of ['currentTime', 'duration', 'playing']) {
        Object.defineProperty(player, property, {
            configurable: true,
            get() {
                throw new Error('native player released');
            }
        });
    }

    assert.deepEqual(binding.snapshot(), {
        positionSeconds: 18,
        durationSeconds: 90,
        isPaused: false
    });
    binding.dispose();
});

test('should sanitize native errors before forwarding them', () => {
    const player = fakePlayer();
    const errors = [];
    const binding = bindNativePlayer(player, { onError: (error) => errors.push(error) });

    player.listeners.get('statusChange')({
        status: 'error',
        error: { message: 'failed https://provider.example/video?token=secret' }
    });

    assert.deepEqual(errors, [{
        errorType: 'native_player',
        errorName: 'PlayerError',
        errorMessage: 'failed <redacted>'
    }]);
    binding.dispose();
});

test('should expose native track selection', () => {
    const player = fakePlayer();
    const binding = bindNativePlayer(player, {});
    const audioTrack = { language: 'ja', label: 'Japanese' };
    const subtitleTrack = { language: 'en', label: 'English' };

    binding.selectAudioTrack(audioTrack);
    binding.selectSubtitleTrack(subtitleTrack);

    assert.equal(player.audioTrack, audioTrack);
    assert.equal(player.subtitleTrack, subtitleTrack);
    binding.selectSubtitleTrack(null);
    assert.equal(player.subtitleTrack, null);
    binding.dispose();
});
