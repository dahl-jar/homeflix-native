import { playbackReportSnapshot } from './playbackReportSnapshot.js';
import { mergeNativeTrackMetadata } from './playbackTrackMetadata.js';
import { hasServerTrackCatalog } from './playbackTrackOverride.js';

function startPlayback(options) {
    if (options.getSnapshot().status === 'playing') return;
    const accepted = options.getAccepted();
    const binding = options.getBinding();
    if (!accepted || !binding) return;
    options.update({ status: 'playing' });
    options.advancePipeline({ type: 'playing' });
    const current = playbackReportSnapshot(binding);
    void accepted.reporter.start(current);
    options.log('playback_started', {
        videoCurrentTime: options.getSnapshot().positionSeconds
    });
}

export function createPlaybackRuntimeCallbacks(options) {
    return {
        onReady({ durationSeconds }) {
            options.update({ status: 'ready', durationSeconds });
            options.log('source_ready', { videoDuration: durationSeconds });
        },
        onPlayingChange(isPlaying) {
            const accepted = options.getAccepted();
            const binding = options.getBinding();
            if (!accepted || !binding) return;
            if (isPlaying) {
                startPlayback(options);
            } else {
                options.update({ status: 'paused' });
                const current = playbackReportSnapshot(binding);
                void accepted.reporter.progress(current, { force: true });
                options.log('playback_paused', {
                    videoCurrentTime: options.getSnapshot().positionSeconds
                });
            }
        },
        onTimeUpdate({ playbackAdvanced, ...position }) {
            options.update(position);
            if (playbackAdvanced) startPlayback(options);
            const accepted = options.getAccepted();
            const binding = options.getBinding();
            if (accepted && binding) {
                void accepted.reporter.progress(playbackReportSnapshot(binding));
            }
        },
        onSourceLoad(fields) {
            options.update({
                ...fields,
                ...mergeNativeTrackMetadata(options.getSnapshot(), fields)
            });
            options.log('source_loaded', {
                durationSeconds: fields.durationSeconds,
                audioTrackCount: fields.audioTracks.length,
                subtitleTrackCount: fields.subtitleTracks.length
            });
        },
        onAudioTrackChange(audioTrack) {
            if (!audioTrack && hasServerTrackCatalog(options.getSnapshot())) return;
            options.update({ selectedAudioTrack: audioTrack });
        },
        onSubtitleTrackChange(subtitleTrack) {
            if (!subtitleTrack && options.getSnapshot().selectedSubtitleTrack?.serverResolved) return;
            options.update({ selectedSubtitleTrack: subtitleTrack });
        },
        onEnded() {
            void options.stop('ended');
        },
        onError(error) {
            void options.recover(error);
        }
    };
}
