export function hasServerTrackCatalog(snapshot) {
    return snapshot.audioTracks.some((track) => track.serverResolved)
        || snapshot.subtitleTracks.some((track) => track.serverResolved);
}

function selectedIndex(track, fallback) {
    return track?.serverResolved ? track.streamIndex : fallback;
}

export function audioTrackOverride(snapshot, context, track) {
    if (!track?.serverResolved || track.streamIndex === context.audioStreamIndex) return null;
    return {
        mediaSourceId: context.mediaSourceId,
        audioStreamIndex: track.streamIndex,
        subtitleStreamIndex: selectedIndex(
            snapshot.selectedSubtitleTrack,
            context.subtitleStreamIndex
        )
    };
}

export function subtitleTrackOverride(snapshot, context, track) {
    if (!hasServerTrackCatalog(snapshot)) return null;
    const subtitleStreamIndex = track?.streamIndex ?? -1;
    if (subtitleStreamIndex === context.subtitleStreamIndex) return null;
    return {
        mediaSourceId: context.mediaSourceId,
        audioStreamIndex: selectedIndex(snapshot.selectedAudioTrack, context.audioStreamIndex),
        subtitleStreamIndex
    };
}
