import { playbackReportSnapshot } from './playbackReportSnapshot.js';

export async function transitionPlaybackTracks(options) {
    const resumeTicks = playbackReportSnapshot(options.binding).positionTicks;
    options.log('track_override_requested', {
        audioStreamIndex: options.trackOverride.audioStreamIndex,
        subtitleStreamIndex: options.trackOverride.subtitleStreamIndex
    });
    await options.stopAccepted(false);
    options.binding.dispose();
    options.clearBinding();
    options.update({ status: 'loading', reason: null });

    try {
        const next = await options.negotiateNext(resumeTicks, {
            trackOverride: options.trackOverride
        });
        await options.loadAccepted(next);
    } catch (error) {
        options.update({
            status: 'failed',
            reason: error instanceof Error ? error.message : 'track override failed'
        });
        options.log('playback_failed', { reason: 'track_override_failed' });
    }
}
