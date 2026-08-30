package app.homeflix.tv.feature.detail

import app.homeflix.tv.core.catalog.MediaItem
import kotlin.math.roundToLong

fun runtimeText(runTimeTicks: Long): String {
    val totalMinutes = (runTimeTicks.toDouble() / TICKS_PER_MINUTE).roundToLong()
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

fun detailChips(item: MediaItem): List<String> =
    buildList {
        item.year?.let { year -> add(year.toString()) }
        item.runTimeTicks?.let { ticks -> add(runtimeText(ticks)) }
        item.officialRating?.let { rating -> add(rating) }
    }

fun starText(item: MediaItem): String? =
    item.communityRating?.let { rating ->
        ((rating * TENTHS).roundToLong() / TENTHS.toDouble()).toString()
    }

fun defaultSeasonIndex(seasons: List<DetailSeason>): Int {
    val firstReal = seasons.indexOfFirst { season -> (season.indexNumber ?: 0) >= 1 }
    return if (firstReal == -1) 0 else firstReal
}

fun playLabel(item: MediaItem): String = if ((item.playbackPositionTicks ?: 0L) > 0L) "Resume" else "Play"

private const val TICKS_PER_MINUTE = 600_000_000L
private const val MINUTES_PER_HOUR = 60L
private const val TENTHS = 10L
