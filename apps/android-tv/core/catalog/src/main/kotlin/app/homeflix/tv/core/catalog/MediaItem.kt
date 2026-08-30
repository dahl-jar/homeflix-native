package app.homeflix.tv.core.catalog

data class MediaItem(
    val id: String,
    val name: String,
    val type: String,
    val seriesId: String?,
    val year: Int?,
    val overview: String?,
    val genres: List<String>,
    val primaryImageUrl: String?,
    val backdropImageUrl: String?,
    val playedPercentage: Float?,
    val seriesName: String? = null,
    val indexNumber: Int? = null,
    val parentIndexNumber: Int? = null,
    val runTimeTicks: Long? = null,
    val playbackPositionTicks: Long? = null,
    val officialRating: String? = null,
    val communityRating: Float? = null,
)
