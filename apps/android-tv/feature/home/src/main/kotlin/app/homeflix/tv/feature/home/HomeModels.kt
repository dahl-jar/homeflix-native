package app.homeflix.tv.feature.home

data class HomeRecommendation(
    val itemId: String,
    val rank: Int,
)

data class HomeMediaItem(
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

enum class HomeRailVariant {
    Poster,
    Landscape,
}

data class HomeRail(
    val id: String,
    val title: String,
    val items: List<HomeMediaItem>,
    val variant: HomeRailVariant,
)

data class HomeContent(
    val featured: List<HomeMediaItem>,
    val rails: List<HomeRail>,
)

data class HomeViewer(
    val id: String,
    val name: String,
    val avatarUrl: String?,
)

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data object Empty : HomeUiState

    data object Error : HomeUiState

    data class Content(
        val value: HomeContent,
    ) : HomeUiState
}
