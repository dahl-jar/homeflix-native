package app.homeflix.tv.feature.player

internal const val DOLBY_VISION_PROFILE_5 = 5
internal const val DOLBY_VISION_PROFILE_7 = 7
internal const val DOLBY_VISION_PROFILE_8 = 8
internal const val DOLBY_VISION_PROFILE_10 = 10

data class VideoDecoderCapability(
    val codec: String,
    val maxWidth: Int,
    val maxHeight: Int,
    val maxFrameRate: Int,
    val maxBitrate: Int,
)

data class TvMediaCapabilities(
    val videoDecoders: List<VideoDecoderCapability>,
    val dolbyVisionProfiles: Set<Int>,
    val displayDolbyVision: Boolean,
    val displayHdr10: Boolean,
    val displayHlg: Boolean,
    val displayHdr10Plus: Boolean,
    val audioPassthroughCodecs: Set<String>,
    val maxAudioChannels: Int,
) {
    val videoCodecs: Set<String> = videoDecoders.mapTo(mutableSetOf(), VideoDecoderCapability::codec)
}
