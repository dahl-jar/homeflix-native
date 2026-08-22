import { bindNativePlayer } from './nativePlayerAdapter.js';
import {
    createPlaybackProgress,
    transitionPlaybackProgress
} from './pipeline/playbackProgress.js';
import { negotiatePlayback } from './playbackCoordinator.js';
import { playbackReportSnapshot } from './playbackReportSnapshot.js';
import { createPlaybackRuntimeCallbacks } from './playbackRuntimeCallbacks.js';
import { serverTrackSnapshot } from './playbackTrackMetadata.js';
import {
    audioTrackOverride,
    hasServerTrackCatalog,
    subtitleTrackOverride
} from './playbackTrackOverride.js';
import { transitionPlaybackTracks } from './playbackTrackTransition.js';

export function createPlaybackRuntime({
    player,
    negotiationOptions,
    onSnapshot,
    negotiate = negotiatePlayback,
    bindPlayer = bindNativePlayer
}) {
    let accepted = null;
    let binding = null;
    let closed = false;
    let recovering = false;
    const rejectedSourceIds = new Set(negotiationOptions.excludedSourceIds ?? []);
    let snapshot = {
        status: 'idle',
        positionSeconds: 0,
        durationSeconds: 0,
        bufferedSeconds: 0,
        audioTracks: [],
        subtitleTracks: [],
        selectedAudioTrack: null,
        selectedSubtitleTrack: null,
        pipeline: createPlaybackProgress(),
        reason: null
    };

    function update(changes) {
        snapshot = { ...snapshot, ...changes };
        onSnapshot({ ...snapshot });
    }

    function log(event, fields = {}) {
        accepted?.telemetry.log(event, fields);
    }

    function advancePipeline(event) {
        update({ pipeline: transitionPlaybackProgress(snapshot.pipeline, event) });
    }

    async function stopAccepted(failed) {
        if (!accepted || !binding) return;
        await accepted.reporter.stop(playbackReportSnapshot(binding, failed));
    }

    function adapterCallbacks() {
        return createPlaybackRuntimeCallbacks({
            getAccepted: () => accepted,
            getBinding: () => binding,
            getSnapshot: () => snapshot,
            update,
            advancePipeline,
            log,
            stop,
            recover
        });
    }

    async function loadAccepted(next) {
        accepted = next;
        binding = bindPlayer(player, adapterCallbacks());
        update({
            status: 'loading',
            reason: null,
            ...serverTrackSnapshot(next.trackMetadata)
        });
        advancePipeline({
            type: 'stage_progress',
            stageId: 'player',
            label: 'Starting player',
            order: 1000,
            status: 'active'
        });
        await binding.load(next.video.source, next.startSeconds);
    }

    async function negotiateNext(startTimeTicks, changes = {}) {
        return negotiate({
            ...negotiationOptions,
            ...changes,
            startTimeTicks,
            excludedSourceIds: rejectedSourceIds,
            pipeline: accepted?.pipeline,
            telemetry: accepted?.telemetry,
            onPipelineProgress: advancePipeline
        });
    }

    async function overrideTracks(trackOverride) {
        if (closed || recovering || !accepted || !binding) return;
        recovering = true;
        try {
            await transitionPlaybackTracks({
                binding,
                trackOverride,
                log,
                stopAccepted,
                clearBinding: () => {
                    binding = null;
                },
                update,
                negotiateNext,
                loadAccepted
            });
        } finally {
            recovering = false;
        }
    }

    async function recover(error) {
        if (closed || recovering || !accepted || !binding) return;
        recovering = true;
        rejectedSourceIds.add(accepted.context.mediaSourceId);
        log('player_failed', error);
        const reason = error.errorMessage ?? 'player failed';
        const resumeTicks = playbackReportSnapshot(binding, true).positionTicks;
        advancePipeline({ type: 'failed', reason });
        update({ status: 'recovering', reason });
        await stopAccepted(true);
        binding.dispose();
        binding = null;
        advancePipeline({ type: 'retry' });

        try {
            await loadAccepted(await negotiateNext(resumeTicks));
        } catch {
            advancePipeline({ type: 'failed', reason: 'no compatible playback source' });
            update({ status: 'failed', reason: 'no compatible playback source' });
            log('playback_failed', { reason: 'no_compatible_source' });
        } finally {
            recovering = false;
        }
    }

    async function stop(status = 'ended') {
        if (closed) return;
        closed = true;
        await stopAccepted(status === 'failed');
        log('pipeline_stopped', {
            videoCurrentTime: snapshot.positionSeconds,
            videoEnded: status === 'ended'
        });
        binding?.dispose();
        binding = null;
        update({ status });
    }

    return {
        async start() {
            if (closed) return;
            update({ status: 'loading', reason: null });
            try {
                const next = await negotiateNext(negotiationOptions.startTimeTicks);
                if (closed) return;
                await loadAccepted(next);
            } catch (error) {
                if (closed) return;
                const reason = error instanceof Error ? error.message : 'playback failed';
                advancePipeline({ type: 'failed', reason });
                update({
                    status: 'failed',
                    reason
                });
                throw error;
            }
        },
        play() {
            binding?.play();
        },
        pause() {
            binding?.pause();
        },
        seekBy(seconds) {
            binding?.seekBy(seconds);
        },
        seekTo(seconds) {
            binding?.seekTo(seconds);
        },
        selectAudioTrack(track) {
            const override = accepted
                ? audioTrackOverride(snapshot, accepted.context, track)
                : null;
            if (override) {
                void overrideTracks(override);
            } else if (!track?.serverResolved) {
                binding?.selectAudioTrack(track);
            }
        },
        selectSubtitleTrack(track) {
            const serverCatalog = hasServerTrackCatalog(snapshot);
            const override = accepted
                ? subtitleTrackOverride(snapshot, accepted.context, track)
                : null;
            if (override) {
                void overrideTracks(override);
            } else if (!serverCatalog) {
                binding?.selectSubtitleTrack(track);
            }
        },
        stop,
        getSnapshot() {
            return { ...snapshot };
        }
    };
}
