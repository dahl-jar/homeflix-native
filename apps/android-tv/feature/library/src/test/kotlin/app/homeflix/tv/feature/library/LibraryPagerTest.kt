package app.homeflix.tv.feature.library

import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.core.catalog.MediaPage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LibraryPagerTest {
    @Test
    fun `should advance start index`() {
        val pager = LibraryPager()

        pager.applyPage(MediaPage(items = mediaItems(3), totalRecordCount = 10))

        assertEquals(3, pager.nextStartIndex())
        assertEquals(10, pager.total)
    }

    @Test
    fun `should load more near end`() {
        val pager = LibraryPager()
        pager.applyPage(MediaPage(items = mediaItems(100), totalRecordCount = 300))

        assertTrue(pager.shouldLoadMore(lastVisibleIndex = 80))
        assertFalse(pager.shouldLoadMore(lastVisibleIndex = 79))
    }

    @Test
    fun `should stop at total`() {
        val pager = LibraryPager()
        pager.applyPage(MediaPage(items = mediaItems(10), totalRecordCount = 10))

        assertFalse(pager.shouldLoadMore(lastVisibleIndex = 9))
    }
}

private fun mediaItems(count: Int): List<MediaItem> =
    List(count) { index ->
        MediaItem(
            id = "item-$index",
            name = "Item $index",
            type = "Movie",
            seriesId = null,
            year = null,
            overview = null,
            genres = emptyList(),
            primaryImageUrl = null,
            backdropImageUrl = null,
            playedPercentage = null,
        )
    }
