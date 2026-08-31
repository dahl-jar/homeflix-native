package app.homeflix.tv.feature.detail

import app.homeflix.tv.core.catalog.MediaItem

data class CastMember(
    val id: String,
    val name: String,
    val imageUrl: String?,
)

data class DetailSeason(
    val id: String,
    val name: String,
    val indexNumber: Int?,
)

data class DetailContent(
    val item: MediaItem,
    val cast: List<CastMember>,
)

internal sealed interface DetailUiState {
    data object Loading : DetailUiState

    data object Error : DetailUiState

    data class Content(
        val value: DetailContent,
    ) : DetailUiState
}
