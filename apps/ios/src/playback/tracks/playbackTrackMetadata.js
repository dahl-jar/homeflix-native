import {
    isSelectableTrack,
    playbackTrackLanguageName,
    playbackTrackLabels
} from './playbackTrackPresentation.js';

function presentationTrack(stream, label) {
    return {
        label,
        language: stream.Language ?? null,
        streamIndex: stream.Index,
        serverResolved: true,
        isDefault: stream.IsDefault === true,
        isForced: stream.IsForced === true,
        name: stream.DisplayTitle ?? stream.Title ?? null
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

function trackLanguage(track) {
    return playbackTrackLanguageName(track);
}

function trackName(track) {
    /* Stryker disable next-line MethodExpression: both names use the same case normalization */
    return String(track.name ?? track.label)
        .trim()
        .toLowerCase();
}

function trackFlag(track, name) {
    const suffix = `${name[0].toUpperCase()}${name.slice(1)}`;
    return Boolean(track[`is${suffix}`]);
}

export function matchingNativeSubtitleTrack(selectedServerTrack, nativeTracks) {
    if (!selectedServerTrack?.serverResolved) return null;
    const language = trackLanguage(selectedServerTrack);
    const candidates = nativeTracks
        .filter((track) => isSelectableTrack(track, 'Subtitle'))
        .filter((track) => trackLanguage(track) === language);

    const forced = trackFlag(selectedServerTrack, 'forced');
    const forcedCandidates = candidates.filter((track) => trackFlag(track, 'forced') === forced);
    const preferredCandidates = forcedCandidates.length > 0
        ? forcedCandidates
        : candidates;
    const isDefault = trackFlag(selectedServerTrack, 'default');
    const defaultCandidates = preferredCandidates.filter((track) =>
        trackFlag(track, 'default') === isDefault
    );
    const rankedCandidates = defaultCandidates.length > 0
        ? defaultCandidates
        : preferredCandidates;
    const selectedName = trackName(selectedServerTrack);
    const namedCandidates = rankedCandidates.filter((track) =>
        trackName(track) === selectedName
    );
    if (namedCandidates.length === 1) return namedCandidates[0];
    return rankedCandidates.length === 1 ? rankedCandidates[0] : null;
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
    const serverSubtitlesAvailable = current.subtitleTracks.some((track) => track.serverResolved);
    return {
        audioTracks: audioAvailable ? audio.tracks : current.audioTracks,
        subtitleTracks: subtitlesAvailable && !serverSubtitlesAvailable
            ? subtitles.tracks
            : current.subtitleTracks,
        selectedAudioTrack: audioAvailable ? audio.selectedTrack : current.selectedAudioTrack,
        selectedSubtitleTrack: subtitlesAvailable && !serverSubtitlesAvailable
            ? subtitles.selectedTrack
            : current.selectedSubtitleTrack
    };
}
