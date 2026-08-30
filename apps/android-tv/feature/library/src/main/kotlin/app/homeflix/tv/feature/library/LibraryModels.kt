package app.homeflix.tv.feature.library

import app.homeflix.tv.core.catalog.MediaItem

internal enum class LibraryFilterKind {
    Sort,
    Genre,
    Decade,
    Rating,
    Status,
}

internal data class PickerRow(
    val key: String?,
    val label: String,
    val selected: Boolean,
)

data class LibraryFilterOptions(
    val genres: List<String>,
    val decades: List<DecadeOption>,
)

data class LibraryPageRequest(
    val libraryId: String,
    val selection: LibraryFilterSelection,
    val startIndex: Int,
    val limit: Int,
)

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data object Error : LibraryUiState

    data class Content(
        val items: List<MediaItem>,
        val total: Int,
    ) : LibraryUiState
}
