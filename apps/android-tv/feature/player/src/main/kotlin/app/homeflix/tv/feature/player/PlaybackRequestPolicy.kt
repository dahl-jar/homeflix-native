package app.homeflix.tv.feature.player

data class PlaybackRequestPolicy(
    val enableDirectPlay: Boolean,
    val enableDirectStream: Boolean,
    val enableTranscoding: Boolean,
    val allowVideoStreamCopy: Boolean,
    val allowAudioStreamCopy: Boolean,
)

fun androidTvPlaybackPolicy(mediaSource: MediaSourceDto? = null): PlaybackRequestPolicy =
    PlaybackRequestPolicy(
        enableDirectPlay = mediaSource == null || mediaSource.isRemote == false && mediaSource.supportsDirectPlay,
        enableDirectStream = false,
        enableTranscoding = true,
        allowVideoStreamCopy = true,
        allowAudioStreamCopy = true,
    )
