package app.homeflix.tv.feature.home

import app.homeflix.tv.core.catalog.MediaItem

data class HomeRecommendation(
    val itemId: String,
    val rank: Int,
)

enum class HomeRailVariant {
    Poster,
    Landscape,
}

data class HomeRail(
    val id: String,
    val title: String,
    val items: List<MediaItem>,
    val variant: HomeRailVariant,
)

data class HomeContent(
    val featured: List<MediaItem>,
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
