import {
    isSelectableTrack,
    playbackTrackLabels
} from './playbackTrackPresentation.js';

function presentationTrack(stream, label) {
    if (!stream) return null;
    return {
        label,
        language: stream.Language ?? null,
        streamIndex: stream.Index,
        serverResolved: true
    };
}

function tracks(mediaSource, type) {
    const streams = (mediaSource.MediaStreams ?? [])
        .filter((stream) => stream.Type === type && isSelectableTrack(stream, type));
    const labels = playbackTrackLabels(streams, type);
    return streams.map((stream, index) => presentationTrack(stream, labels[index]));
}

export function playbackTrackMetadata(mediaSource, audioStreamIndex, subtitleStreamIndex) {
    const audioTracks = tracks(mediaSource, 'Audio');
    const subtitleTracks = tracks(mediaSource, 'Subtitle');
    return {
        audioTracks,
        subtitleTracks,
        selectedAudioTrack: audioTracks.find((track) => track.streamIndex === audioStreamIndex) ?? null,
        selectedSubtitleTrack: subtitleTracks.find((track) =>
            track.streamIndex === subtitleStreamIndex
        ) ?? null
    };
}

export function serverTrackSnapshot(metadata) {
    return {
        audioTracks: metadata?.audioTracks ?? [],
        subtitleTracks: metadata?.subtitleTracks ?? [],
        selectedAudioTrack: metadata?.selectedAudioTrack ?? null,
        selectedSubtitleTrack: metadata?.selectedSubtitleTrack ?? null
    };
}

function sameNativeTrack(track, selected) {
    return track === selected || (track.id != null && track.id === selected?.id);
}

function nativeTrackSet(nativeTracks, selectedTrack, type) {
    const selectable = nativeTracks.filter((track) => isSelectableTrack(track, type));
    const labels = playbackTrackLabels(selectable, type);
    const entries = selectable
        .map((track, index) => ({
            original: track,
            presented: { ...track, label: labels[index] }
        }));
    return {
        tracks: entries.map(({ presented }) => presented),
        selectedTrack: entries.find(({ original }) =>
            sameNativeTrack(original, selectedTrack)
        )?.presented ?? null
    };
}

export function mergeNativeTrackMetadata(current, native) {
    const audio = nativeTrackSet(native.audioTracks, native.selectedAudioTrack, 'Audio');
    const subtitles = nativeTrackSet(
        native.subtitleTracks,
        native.selectedSubtitleTrack,
        'Subtitle'
    );
    const audioAvailable = audio.tracks.length > 0;
    const subtitlesAvailable = subtitles.tracks.length > 0;
    return {
        audioTracks: audioAvailable ? audio.tracks : current.audioTracks,
        subtitleTracks: subtitlesAvailable ? subtitles.tracks : current.subtitleTracks,
        selectedAudioTrack: audioAvailable ? audio.selectedTrack : current.selectedAudioTrack,
        selectedSubtitleTrack: subtitlesAvailable
            ? subtitles.selectedTrack
            : current.selectedSubtitleTrack
    };
}
