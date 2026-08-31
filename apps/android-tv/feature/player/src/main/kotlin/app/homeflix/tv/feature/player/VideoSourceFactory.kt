package app.homeflix.tv.feature.player

import java.net.URLEncoder

data class ReleasedPlayback(
    val itemId: String,
    val mediaSourceId: String,
    val playSessionId: String,
    val playMethod: PlayMethod,
    val audioStreamIndex: Int,
    val subtitleStreamIndex: Int,
    val pipelineId: String,
    val attemptId: String,
    val transcodingUrl: String?,
    val transcodingSubProtocol: String?,
)

data class PlayerMediaSource(
    val url: String,
    val hls: Boolean,
)

private val HLS_URL_PATTERN = Regex("""\.m3u8($|\?)""", RegexOption.IGNORE_CASE)

fun playbackMethod(source: MediaSourceDto): PlayMethod? =
    when {
        source.supportsDirectPlay -> PlayMethod.DIRECT_PLAY
        source.supportsDirectStream -> PlayMethod.DIRECT_STREAM
        source.supportsTranscoding -> PlayMethod.TRANSCODE
        else -> null
    }

fun videoSource(
    baseUrl: String,
    playback: ReleasedPlayback,
): PlayerMediaSource {
    val normalizedBase = baseUrl.trimEnd('/')
    if (playback.playMethod == PlayMethod.DIRECT_PLAY) {
        val query =
            listOf(
                "Static" to "true",
                "MediaSourceId" to playback.mediaSourceId,
                "PlaySessionId" to playback.playSessionId,
                "AudioStreamIndex" to playback.audioStreamIndex.toString(),
                "SubtitleStreamIndex" to playback.subtitleStreamIndex.toString(),
                "PlaybackPipelineId" to playback.pipelineId,
                "PlaybackAttemptId" to playback.attemptId,
            ).joinToString("&") { (name, value) -> "$name=${URLEncoder.encode(value, "UTF-8")}" }
        return PlayerMediaSource(
            url = "$normalizedBase/Videos/${playback.itemId}/stream?$query",
            hls = false,
        )
    }
    val transcodingUrl =
        checkNotNull(playback.transcodingUrl) { "released source has no transcoding url" }
    val absolute =
        if (transcodingUrl.startsWith("http")) transcodingUrl else "$normalizedBase$transcodingUrl"
    return PlayerMediaSource(
        url = absolute,
        hls = HLS_URL_PATTERN.containsMatchIn(absolute) || playback.transcodingSubProtocol == "hls",
    )
}
