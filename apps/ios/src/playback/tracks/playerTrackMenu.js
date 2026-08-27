function trackLabel(track, fallback) {
    return track.label || track.name || track.language || fallback;
}

export function audioTrackEntries(snapshot) {
    return snapshot.audioTracks.map((track, index) => ({
        key: `audio:${index}`,
        label: trackLabel(track, `Track ${index + 1}`)
    }));
}

export function subtitleTrackEntries(snapshot) {
    const subtitles = snapshot.subtitleTracks.map((track, index) => ({
        key: `subtitle:${index}`,
        label: trackLabel(track, `Track ${index + 1}`)
    }));
    return [{ key: 'subtitle:off', label: 'Off' }, ...subtitles];
}

export function selectedTrackKey(snapshot, entry) {
    if (entry.key === 'subtitle:off') return snapshot.selectedSubtitleTrack === null;
    const [type, indexText] = entry.key.split(':');
    const tracks = type === 'audio' ? snapshot.audioTracks : snapshot.subtitleTracks;
    const selected = type === 'audio' ? snapshot.selectedAudioTrack : snapshot.selectedSubtitleTrack;
    return tracks[Number(indexText)] === selected;
}
