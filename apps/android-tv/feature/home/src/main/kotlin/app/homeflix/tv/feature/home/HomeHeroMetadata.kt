package app.homeflix.tv.feature.home

import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

internal object HomeHeroMetadata {
    private const val TICKS_PER_SECOND = 10_000_000L
    private const val TICKS_PER_MINUTE = TICKS_PER_SECOND * 60
    private const val MINUTES_PER_HOUR = 60L
    private const val TENTHS_SCALE = 10
    private const val EPISODE_TYPE = "Episode"
    private val ENDS_AT_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    fun segments(
        item: HomeMediaItem,
        now: ZonedDateTime,
    ): List<String> =
        buildList {
            episodeLabel(item)?.let(::add)
            item.year?.let { add(it.toString()) }
            item.runTimeTicks?.let { add(runtimeLabel(it)) }
            remainingTicks(item)?.let { add("${it / TICKS_PER_MINUTE}m left") }
            item.officialRating?.let(::add)
            communityRatingLabel(item)?.let(::add)
            item.runTimeTicks?.let { runtime -> add(endsAtLabel(item, runtime, now)) }
        }

    fun communityRatingLabel(item: HomeMediaItem): String? = item.communityRating?.let(::formatRating)

    private fun formatRating(rating: Float): String {
        val tenths = (rating * TENTHS_SCALE).roundToInt()
        return "${tenths / TENTHS_SCALE}.${tenths % TENTHS_SCALE}"
    }

    private fun episodeLabel(item: HomeMediaItem): String? {
        val season = item.parentIndexNumber
        val episode = item.indexNumber
        return if (item.type == EPISODE_TYPE && season != null && episode != null) {
            "S$season E$episode"
        } else {
            null
        }
    }

    private fun runtimeLabel(runTimeTicks: Long): String {
        val minutes = runTimeTicks / TICKS_PER_MINUTE
        val hours = minutes / MINUTES_PER_HOUR
        val remainder = minutes % MINUTES_PER_HOUR
        return if (hours > 0) "${hours}h ${remainder}m" else "${minutes}m"
    }

    private fun remainingTicks(item: HomeMediaItem): Long? {
        val runtime = item.runTimeTicks
        val position = item.playbackPositionTicks
        if (runtime == null || position == null) return null
        return if (position > 0 && position < runtime) runtime - position else null
    }

    private fun endsAtLabel(
        item: HomeMediaItem,
        runtime: Long,
        now: ZonedDateTime,
    ): String {
        val ticksToPlay = remainingTicks(item) ?: runtime
        val endsAt = now.plus(Duration.ofSeconds(ticksToPlay / TICKS_PER_SECOND))
        return "Ends at ${ENDS_AT_FORMATTER.format(endsAt)}"
    }
}
