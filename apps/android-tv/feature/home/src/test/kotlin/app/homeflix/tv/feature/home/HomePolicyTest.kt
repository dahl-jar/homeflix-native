package app.homeflix.tv.feature.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HomePolicyTest {
    @Test
    fun `should rank and cap featured items`() {
        val recommendations =
            (1..10)
                .map { rank -> HomeRecommendation(itemId = "item-$rank", rank = rank) }
                .reversed()
        val resolved =
            recommendations.associate { recommendation ->
                recommendation.itemId to mediaItem(recommendation.itemId)
            }

        val featured = HomePolicy.rankFeatured(recommendations, resolved)

        assertEquals(8, featured.size)
        assertEquals((1..8).map { rank -> "item-$rank" }, featured.map(HomeMediaItem::id))
    }

    @Test
    fun `should remove unresolved features and empty rails`() {
        val recommendations =
            listOf(
                HomeRecommendation(itemId = "missing", rank = 1),
                HomeRecommendation(itemId = "item-two", rank = 2),
            )
        val rails =
            listOf(
                HomeRail(id = "empty", title = "Empty", items = emptyList(), variant = HomeRailVariant.Poster),
                HomeRail(
                    id = "recent",
                    title = "Recent",
                    items = listOf(mediaItem("item-two")),
                    variant = HomeRailVariant.Poster,
                ),
            )

        val featured = HomePolicy.rankFeatured(recommendations, mapOf("item-two" to mediaItem("item-two")))
        val nonEmptyRails = HomePolicy.nonEmptyRails(rails)

        assertEquals(listOf("item-two"), featured.map(HomeMediaItem::id))
        assertEquals(listOf("recent"), nonEmptyRails.map(HomeRail::id))
    }

    @Test
    fun `should prefer continue watching item as hero`() {
        val resumeItem = mediaItem("resume-one")
        val content =
            HomeContent(
                featured = listOf(mediaItem("featured-one")),
                rails =
                    listOf(
                        HomeRail(
                            id = "continue",
                            title = "Continue watching",
                            items = listOf(resumeItem),
                            variant = HomeRailVariant.Poster,
                        ),
                    ),
            )

        assertEquals(resumeItem, HomePolicy.initialHero(content))
    }

    @Test
    fun `should fall back to featured hero without resume items`() {
        val featuredItem = mediaItem("featured-one")
        val content =
            HomeContent(
                featured = listOf(featuredItem),
                rails =
                    listOf(
                        HomeRail(
                            id = "recent",
                            title = "Recent",
                            items = listOf(mediaItem("recent")),
                            variant = HomeRailVariant.Poster,
                        ),
                    ),
            )

        assertEquals(featuredItem, HomePolicy.initialHero(content))
    }

    @Test
    fun `should choose first item as hero`() {
        val recentItem = mediaItem("recent")
        val content =
            HomeContent(
                featured = emptyList(),
                rails =
                    listOf(
                        HomeRail(
                            id = "recent",
                            title = "Recent",
                            items = listOf(recentItem),
                            variant = HomeRailVariant.Poster,
                        ),
                    ),
            )

        assertEquals(recentItem, HomePolicy.initialHero(content))
        assertNull(HomePolicy.initialHero(HomeContent(featured = emptyList(), rails = emptyList())))
    }

    @Test
    fun `should use series id for episodes`() {
        val episode = mediaItem(id = "episode-one", type = "Episode", seriesId = "series-one")

        assertEquals("series-one", HomePolicy.selectionId(episode))
        assertEquals("movie-one", HomePolicy.selectionId(mediaItem("movie-one")))
        assertEquals("movie-two", HomePolicy.selectionId(mediaItem("movie-two", seriesId = "series-two")))
        assertEquals("episode-two", HomePolicy.selectionId(mediaItem("episode-two", type = "Episode", seriesId = "")))
    }
}

private fun mediaItem(
    id: String,
    type: String = "Movie",
    seriesId: String? = null,
): HomeMediaItem =
    HomeMediaItem(
        id = id,
        name = id,
        type = type,
        seriesId = seriesId,
        year = 2026,
        overview = "A private-library title.",
        genres = listOf("Drama"),
        primaryImageUrl = "http://server/Items/$id/Images/Primary",
        backdropImageUrl = "http://server/Items/$id/Images/Backdrop/0",
        playedPercentage = null,
    )
