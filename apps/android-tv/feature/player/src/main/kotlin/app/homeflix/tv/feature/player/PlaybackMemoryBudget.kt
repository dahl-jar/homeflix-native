package app.homeflix.tv.feature.player

object PlaybackMemoryBudget {
    const val TARGET_BUFFER_BYTES = 32 * 1_024 * 1_024
    const val MINIMUM_BUFFER_MS = 15_000
    const val MAXIMUM_BUFFER_MS = 30_000
    const val BUFFER_FOR_PLAYBACK_MS = 1_500
    const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 3_000
}
