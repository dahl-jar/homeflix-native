package app.homeflix.tv.feature.library

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LibraryFiltersTest {
    @Test
    fun `should default to top rated`() {
        val query = LibraryFilters.buildLibraryQuery(LibraryFilterSelection())

        assertEquals(mapOf("sortBy" to "CommunityRating", "sortOrder" to "Descending"), query)
    }

    @Test
    fun `should expand decade years`() {
        val decade = LibraryFilters.decadesFromYears(listOf(1994)).single()

        val query = LibraryFilters.buildLibraryQuery(LibraryFilterSelection(decade = decade))

        assertEquals("1990,1991,1992,1993,1994,1995,1996,1997,1998,1999", query["years"])
    }

    @Test
    fun `should map rating and status`() {
        val selection =
            LibraryFilterSelection(
                genre = "Drama",
                rating = LibraryFilters.ratingOptions.first { it.key == "8" },
                status = LibraryFilters.statusOptions.first { it.key == "unwatched" },
            )

        val query = LibraryFilters.buildLibraryQuery(selection)

        assertEquals("Drama", query["genres"])
        assertEquals("8", query["minCommunityRating"])
        assertEquals("false", query["isPlayed"])
    }

    @Test
    fun `should dedupe decades`() {
        val decades = LibraryFilters.decadesFromYears(listOf(1994, 1999, 2003))

        assertEquals(listOf("2000s", "1990s"), decades.map(DecadeOption::label))
    }
}
