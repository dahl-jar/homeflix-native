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
    val timelineMode: TimelineMode,
)

enum class TimelineMode {
    ABSOLUTE_ITEM,
    SERVER_PRESEEKED,
}

data class PlaybackTimeline(
    val mode: TimelineMode,
    val originSeconds: Double,
) {
    fun playerPositionSeconds(itemPositionSeconds: Double): Double =
        when (mode) {
            TimelineMode.ABSOLUTE_ITEM -> itemPositionSeconds
            TimelineMode.SERVER_PRESEEKED -> (itemPositionSeconds - originSeconds).coerceAtLeast(0.0)
        }

    fun itemPositionSeconds(playerPositionSeconds: Double): Double =
        when (mode) {
            TimelineMode.ABSOLUTE_ITEM -> playerPositionSeconds
            TimelineMode.SERVER_PRESEEKED -> originSeconds + playerPositionSeconds
        }

    fun itemDurationSeconds(playerDurationSeconds: Double): Double =
        when {
            playerDurationSeconds <= 0.0 -> 0.0
            mode == TimelineMode.ABSOLUTE_ITEM -> playerDurationSeconds
            else -> originSeconds + playerDurationSeconds
        }
}

fun PlayerMediaSource.timeline(itemStartSeconds: Double): PlaybackTimeline =
    PlaybackTimeline(mode = timelineMode, originSeconds = itemStartSeconds)

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
            timelineMode = TimelineMode.ABSOLUTE_ITEM,
        )
    }
    val transcodingUrl =
        checkNotNull(playback.transcodingUrl) { "released source has no transcoding url" }
    val absolute =
        if (transcodingUrl.startsWith("http")) transcodingUrl else "$normalizedBase$transcodingUrl"
    val hls = HLS_URL_PATTERN.containsMatchIn(absolute) || playback.transcodingSubProtocol == "hls"
    return PlayerMediaSource(
        url = absolute,
        hls = hls,
        timelineMode = if (hls) TimelineMode.ABSOLUTE_ITEM else TimelineMode.SERVER_PRESEEKED,
    )
}
