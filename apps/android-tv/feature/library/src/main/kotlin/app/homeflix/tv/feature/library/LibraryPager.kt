package app.homeflix.tv.feature.library

import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.core.catalog.MediaPage

class LibraryPager(
    val pageSize: Int = PAGE_SIZE,
) {
    private val loaded = mutableListOf<MediaItem>()

    var total: Int = 0
        private set

    val items: List<MediaItem>
        get() = loaded.toList()

    fun nextStartIndex(): Int = loaded.size

    fun applyPage(page: MediaPage) {
        loaded += page.items
        total = page.totalRecordCount
    }

    fun shouldLoadMore(lastVisibleIndex: Int): Boolean {
        val nearLoadedEnd = lastVisibleIndex >= loaded.size - LOAD_MORE_THRESHOLD
        return loaded.size < total && nearLoadedEnd
    }

    companion object {
        const val PAGE_SIZE = 100
        private const val LOAD_MORE_THRESHOLD = 20
    }
}
