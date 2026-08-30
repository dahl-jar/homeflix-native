package app.homeflix.tv.feature.player

private const val MONO_CHANNELS = 1
private const val STEREO_CHANNELS = 2
private const val SURROUND_CHANNELS = 6
private const val FULL_SURROUND_CHANNELS = 8

private val LANGUAGE_NAMES =
    mapOf(
        "ar" to "Arabic",
        "ara" to "Arabic",
        "de" to "German",
        "deu" to "German",
        "ger" to "German",
        "en" to "English",
        "eng" to "English",
        "es" to "Spanish",
        "spa" to "Spanish",
        "fi" to "Finnish",
        "fin" to "Finnish",
        "fr" to "French",
        "fra" to "French",
        "fre" to "French",
        "hi" to "Hindi",
        "hin" to "Hindi",
        "it" to "Italian",
        "ita" to "Italian",
        "ja" to "Japanese",
        "jpn" to "Japanese",
        "ko" to "Korean",
        "kor" to "Korean",
        "no" to "Norwegian",
        "nor" to "Norwegian",
        "nob" to "Norwegian",
        "nno" to "Norwegian",
        "pt" to "Portuguese",
        "por" to "Portuguese",
        "ru" to "Russian",
        "rus" to "Russian",
        "sv" to "Swedish",
        "swe" to "Swedish",
        "th" to "Thai",
        "tha" to "Thai",
        "zh" to "Chinese",
        "chi" to "Chinese",
        "cmn" to "Chinese",
        "yue" to "Chinese",
        "zho" to "Chinese",
    )

private val COMMENTARY_PATTERN = Regex("""\bcomment(?:ary)?\b""", RegexOption.IGNORE_CASE)
private val FORCED_PATTERN = Regex("""\bforced\b""", RegexOption.IGNORE_CASE)
private val HEARING_IMPAIRED_PATTERN = Regex("""\b(?:cc|sdh|hearing impaired)\b""", RegexOption.IGNORE_CASE)
private val SIGNS_OR_SONGS_PATTERN = Regex("""\b(?:signs?|songs?)\b""", RegexOption.IGNORE_CASE)

enum class TrackKind {
    AUDIO,
    SUBTITLE,
}

private fun trackText(stream: MediaStreamDto): String =
    "${stream.displayTitle.orEmpty()} ${stream.title.orEmpty()}".trim()

private fun languageName(stream: MediaStreamDto): String {
    val language =
        stream.language
            .orEmpty()
            .trim()
            .lowercase()
            .split('-', '_')
            .first()
    LANGUAGE_NAMES[language]?.let { return it }
    val text = trackText(stream)
    return LANGUAGE_NAMES.values.distinct().find { name ->
        Regex("""\b$name\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)
    } ?: "Unknown"
}

private fun audioLayout(stream: MediaStreamDto): String? =
    when (stream.channels) {
        MONO_CHANNELS -> "Mono"
        STEREO_CHANNELS -> "Stereo"
        SURROUND_CHANNELS -> "5.1"
        FULL_SURROUND_CHANNELS -> "7.1"
        else -> null
    }

private fun subtitleQualifiers(stream: MediaStreamDto): List<String> {
    val text = trackText(stream)
    return listOfNotNull(
        "Forced".takeIf { stream.isForced || FORCED_PATTERN.containsMatchIn(text) },
        "SDH".takeIf { stream.isHearingImpaired || HEARING_IMPAIRED_PATTERN.containsMatchIn(text) },
    )
}

fun isSelectableTrack(
    stream: MediaStreamDto,
    kind: TrackKind,
): Boolean {
    val text = trackText(stream)
    if (COMMENTARY_PATTERN.containsMatchIn(text)) return false
    return kind != TrackKind.SUBTITLE || !SIGNS_OR_SONGS_PATTERN.containsMatchIn(text)
}

private fun baseLabel(
    stream: MediaStreamDto,
    kind: TrackKind,
): String {
    val qualifiers =
        when (kind) {
            TrackKind.AUDIO -> listOfNotNull(audioLayout(stream))
            TrackKind.SUBTITLE -> subtitleQualifiers(stream)
        }
    return (listOf(languageName(stream)) + qualifiers).joinToString(" · ")
}

private fun deliveryLabel(
    stream: MediaStreamDto,
    kind: TrackKind,
): String? =
    when {
        kind != TrackKind.SUBTITLE -> null
        stream.isExternal == true -> "External"
        stream.isExternal == false -> "Embedded"
        else -> null
    }

fun playbackTrackLabels(
    streams: List<MediaStreamDto>,
    kind: TrackKind,
): List<String> {
    val baseLabels = streams.map { baseLabel(it, kind) }
    val baseCounts = baseLabels.groupingBy { it }.eachCount()
    val detailedLabels =
        baseLabels.mapIndexed { index, label ->
            if (baseCounts.getValue(label) == 1) {
                label
            } else {
                deliveryLabel(streams[index], kind)?.let { "$label · $it" } ?: label
            }
        }
    val detailedCounts = detailedLabels.groupingBy { it }.eachCount()
    val occurrences = mutableMapOf<String, Int>()
    return detailedLabels.map { label ->
        if (detailedCounts.getValue(label) == 1) {
            label
        } else {
            val occurrence = (occurrences[label] ?: 0) + 1
            occurrences[label] = occurrence
            "$label $occurrence"
        }
    }
}
