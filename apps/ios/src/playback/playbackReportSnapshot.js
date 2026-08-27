const TICKS_PER_SECOND = 10_000_000;

export function playbackReportSnapshot(binding, failed = false) {
    const snapshot = binding.snapshot();
    return {
        positionTicks: Math.round(snapshot.positionSeconds * TICKS_PER_SECOND),
        isPaused: snapshot.isPaused,
        failed
    };
}
