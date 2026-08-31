package app.homeflix.tv.feature.library

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CancellationException

internal suspend fun loadPage(
    gateway: LibraryGateway,
    userId: String,
    libraryId: String,
    selection: LibraryFilterSelection,
    pager: LibraryPager,
): LibraryUiState.Content? =
    try {
        val page =
            gateway.fetchPage(
                userId = userId,
                request =
                    LibraryPageRequest(
                        libraryId = libraryId,
                        selection = selection,
                        startIndex = pager.nextStartIndex(),
                        limit = pager.pageSize,
                    ),
            )
        pager.applyPage(page)
        LibraryUiState.Content(items = pager.items, total = pager.total)
    } catch (failure: CancellationException) {
        throw failure
    } catch (_: Exception) {
        null
    }

internal suspend fun loadFilterOptions(
    gateway: LibraryGateway,
    userId: String,
    libraryId: String,
): LibraryFilterOptions =
    try {
        gateway.fetchFilterOptions(userId, libraryId)
    } catch (failure: CancellationException) {
        throw failure
    } catch (_: Exception) {
        LibraryFilterOptions(genres = emptyList(), decades = emptyList())
    }

internal suspend fun observeLoadMore(
    gridState: LazyGridState,
    pager: LibraryPager,
    append: suspend () -> Unit,
) {
    snapshotFlow {
        gridState.layoutInfo.visibleItemsInfo
            .lastOrNull()
            ?.index ?: 0
    }.collect { lastVisibleIndex ->
        if (pager.shouldLoadMore(lastVisibleIndex)) {
            append()
        }
    }
}
