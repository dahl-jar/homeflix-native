const LANGUAGE_NAMES = Object.freeze({
    ar: 'Arabic', ara: 'Arabic',
    de: 'German', deu: 'German', ger: 'German',
    en: 'English', eng: 'English',
    es: 'Spanish', spa: 'Spanish',
    fi: 'Finnish', fin: 'Finnish',
    fr: 'French', fra: 'French', fre: 'French',
    hi: 'Hindi', hin: 'Hindi',
    it: 'Italian', ita: 'Italian',
    ja: 'Japanese', jpn: 'Japanese',
    ko: 'Korean', kor: 'Korean',
    no: 'Norwegian', nor: 'Norwegian', nob: 'Norwegian', nno: 'Norwegian',
    pt: 'Portuguese', por: 'Portuguese',
    ru: 'Russian', rus: 'Russian',
    sv: 'Swedish', swe: 'Swedish',
    th: 'Thai', tha: 'Thai',
    zh: 'Chinese', chi: 'Chinese', cmn: 'Chinese', yue: 'Chinese', zho: 'Chinese'
});

const COMMENTARY_PATTERN = /\bcomment(?:ary)?\b/i;
const FORCED_PATTERN = /\bforced\b/i;
const HEARING_IMPAIRED_PATTERN = /\b(?:cc|sdh|hearing impaired)\b/i;
const SIGNS_OR_SONGS_PATTERN = /\b(?:signs?|songs?)\b/i;

function trackText(stream) {
    return `${stream.DisplayTitle ?? stream.label ?? ''} ${stream.Title ?? stream.name ?? ''}`.trim();
}

function languageName(stream) {
    const language = String(stream.Language ?? stream.language ?? '')
        .trim()
        .toLowerCase()
        .split(/[-_]/u)[0];
    if (LANGUAGE_NAMES[language]) return LANGUAGE_NAMES[language];
    const text = trackText(stream);
    return Object.values(LANGUAGE_NAMES).find((name) =>
        new RegExp(`\\b${name}\\b`, 'iu').test(text)
    ) ?? 'Unknown';
}

function audioLayout(stream) {
    const channels = stream.Channels ?? stream.channelCount;
    if (channels === 1) return 'Mono';
    if (channels === 2) return 'Stereo';
    if (channels === 6) return '5.1';
    if (channels === 8) return '7.1';
    return null;
}

function subtitleQualifiers(stream) {
    const text = trackText(stream);
    return [
        stream.IsForced || stream.isForced || FORCED_PATTERN.test(text) ? 'Forced' : null,
        stream.IsHearingImpaired || stream.isHearingImpaired
            || HEARING_IMPAIRED_PATTERN.test(text) ? 'SDH' : null
    ].filter(Boolean);
}

export function isSelectableTrack(stream, type) {
    const text = trackText(stream);
    if (COMMENTARY_PATTERN.test(text)) return false;
    return type !== 'Subtitle' || !SIGNS_OR_SONGS_PATTERN.test(text);
}

function playbackTrackLabel(stream, type) {
    const qualifiers = type === 'Audio'
        ? [audioLayout(stream)].filter(Boolean)
        : subtitleQualifiers(stream);
    return [languageName(stream), ...qualifiers].join(' · ');
}

function deliveryLabel(stream, type) {
    if (type !== 'Subtitle') return null;
    const external = stream.IsExternal ?? stream.isExternal;
    if (external === true) return 'External';
    if (external === false) return 'Embedded';
    return null;
}

function labelCounts(labels) {
    return labels.reduce((counts, label) => {
        counts.set(label, (counts.get(label) ?? 0) + 1);
        return counts;
    }, new Map());
}

export function playbackTrackLabels(streams, type) {
    const baseLabels = streams.map((stream) => playbackTrackLabel(stream, type));
    const baseCounts = labelCounts(baseLabels);
    const detailedLabels = baseLabels.map((label, index) => {
        if (baseCounts.get(label) === 1) return label;
        const delivery = deliveryLabel(streams[index], type);
        return delivery ? `${label} · ${delivery}` : label;
    });
    const detailedCounts = labelCounts(detailedLabels);
    const occurrences = new Map();
    return detailedLabels.map((label) => {
        if (detailedCounts.get(label) === 1) return label;
        const occurrence = (occurrences.get(label) ?? 0) + 1;
        occurrences.set(label, occurrence);
        return `${label} ${occurrence}`;
    });
}
