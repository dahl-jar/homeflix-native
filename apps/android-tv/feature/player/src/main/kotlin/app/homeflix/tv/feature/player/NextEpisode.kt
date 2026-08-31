package app.homeflix.tv.feature.player

private const val COUNTDOWN_SECONDS = 10

fun selectFollowingEpisode(
    items: List<PlayableItem>,
    currentItemId: String,
): PlayableItem? {
    val currentIndex = items.indexOfFirst { it.id == currentItemId }
    if (currentIndex < 0) return null
    return items
        .drop(currentIndex + 1)
        .firstOrNull { it.type == "Episode" && !it.isMissing }
}

data class NextEpisodeView(
    val active: Boolean,
    val remainingSeconds: Int,
    val nextEpisode: PlayableItem?,
)

class NextEpisodeCountdown(
    private val itemId: String,
    private val nextEpisode: PlayableItem?,
    private val onAdvance: (PlayableItem) -> Unit,
) {
    private var countdownKey: String? = null
    private var remaining = COUNTDOWN_SECONDS
    private var cancelled = false
    private var advancedKey: String? = null

    fun update(
        activeSegmentOutroId: String?,
        ended: Boolean,
    ): NextEpisodeView {
        val key =
            when {
                nextEpisode == null -> null
                activeSegmentOutroId != null -> "$itemId:$activeSegmentOutroId"
                ended -> "$itemId:ended"
                else -> null
            }
        if (key != countdownKey) {
            countdownKey = key
            remaining = COUNTDOWN_SECONDS
            cancelled = false
        }
        return view()
    }

    fun tick(): NextEpisodeView {
        val key = countdownKey
        if (key == null || cancelled) return view()
        if (remaining > 0) remaining -= 1
        if (remaining == 0) advanceOnce(key)
        return view()
    }

    fun cancel() {
        if (countdownKey != null) cancelled = true
    }

    fun playNext() {
        val episode = nextEpisode ?: return
        val key = countdownKey ?: itemId
        if (advancedKey == key) return
        advancedKey = key
        onAdvance(episode)
    }

    private fun advanceOnce(key: String) {
        val episode = nextEpisode ?: return
        if (advancedKey == key) return
        advancedKey = key
        onAdvance(episode)
    }

    private fun view(): NextEpisodeView =
        NextEpisodeView(
            active = countdownKey != null && !cancelled,
            remainingSeconds = remaining,
            nextEpisode = nextEpisode,
        )
}
