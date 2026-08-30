package app.homeflix.tv.feature.player

data class TvMediaCapabilities(
    val videoCodecs: Set<String>,
    val dolbyVisionProfiles: Set<Int>,
    val displayDolbyVision: Boolean,
    val displayHdr10: Boolean,
    val displayHlg: Boolean,
    val displayHdr10Plus: Boolean,
    val audioPassthroughCodecs: Set<String>,
    val maxAudioChannels: Int,
)
