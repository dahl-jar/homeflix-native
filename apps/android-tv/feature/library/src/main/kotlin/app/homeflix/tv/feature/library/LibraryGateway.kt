package app.homeflix.tv.feature.library

import app.homeflix.tv.core.catalog.LibrarySummary
import app.homeflix.tv.core.catalog.MediaPage

interface LibraryGateway {
    suspend fun fetchLibraries(userId: String): List<LibrarySummary>

    suspend fun fetchPage(
        userId: String,
        request: LibraryPageRequest,
    ): MediaPage

    suspend fun fetchFilterOptions(
        userId: String,
        libraryId: String,
    ): LibraryFilterOptions
}
