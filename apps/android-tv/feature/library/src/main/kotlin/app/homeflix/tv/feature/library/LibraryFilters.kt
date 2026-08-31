package app.homeflix.tv.feature.library

data class SortOption(
    val key: String,
    val label: String,
    val sortBy: String,
    val sortOrder: String,
)

data class DecadeOption(
    val key: String,
    val label: String,
    val start: Int,
)

data class RatingOption(
    val key: String,
    val label: String,
    val minCommunityRating: Int,
)

data class StatusOption(
    val key: String,
    val label: String,
    val isPlayed: Boolean,
)

data class LibraryFilterSelection(
    val sort: SortOption = LibraryFilters.sortOptions.first(),
    val genre: String? = null,
    val decade: DecadeOption? = null,
    val rating: RatingOption? = null,
    val status: StatusOption? = null,
) {
    val hasRefinements: Boolean
        get() = genre != null || decade != null || rating != null || status != null
}

object LibraryFilters {
    private const val YEARS_PER_DECADE = 10

    val sortOptions =
        listOf(
            SortOption(key = "rating", label = "Top Rated", sortBy = "CommunityRating", sortOrder = "Descending"),
            SortOption(key = "az", label = "A–Z", sortBy = "SortName", sortOrder = "Ascending"),
            SortOption(key = "recent", label = "Recently Added", sortBy = "DateCreated", sortOrder = "Descending"),
        )

    val ratingOptions =
        listOf(
            RatingOption(key = "7", label = "7+", minCommunityRating = 7),
            RatingOption(key = "8", label = "8+", minCommunityRating = 8),
            RatingOption(key = "9", label = "9+", minCommunityRating = 9),
        )

    val statusOptions =
        listOf(
            StatusOption(key = "unwatched", label = "Unwatched", isPlayed = false),
            StatusOption(key = "watched", label = "Watched", isPlayed = true),
        )

    fun decadesFromYears(years: List<Int>): List<DecadeOption> =
        years
            .map { year -> year / YEARS_PER_DECADE * YEARS_PER_DECADE }
            .distinct()
            .sortedDescending()
            .map { start -> DecadeOption(key = start.toString(), label = "${start}s", start = start) }

    fun buildLibraryQuery(selection: LibraryFilterSelection): Map<String, String> =
        buildMap {
            put("sortBy", selection.sort.sortBy)
            put("sortOrder", selection.sort.sortOrder)
            selection.genre?.let { genre -> put("genres", genre) }
            selection.decade?.let { decade ->
                val years = List(YEARS_PER_DECADE) { offset -> decade.start + offset }
                put("years", years.joinToString(","))
            }
            selection.rating?.let { rating -> put("minCommunityRating", rating.minCommunityRating.toString()) }
            selection.status?.let { status -> put("isPlayed", status.isPlayed.toString()) }
        }
}
