package app.homeflix.tv.feature.home

import app.homeflix.tv.core.catalog.MediaItem

object HomePolicy {
    const val CONTINUE_RAIL_ID = "continue"

    private const val FEATURED_LIMIT = 8

    fun rankFeatured(
        recommendations: List<HomeRecommendation>,
        resolvedItems: Map<String, MediaItem>,
    ): List<MediaItem> =
        recommendations
            .sortedBy(HomeRecommendation::rank)
            .mapNotNull { resolvedItems[it.itemId] }
            .distinctBy(MediaItem::id)
            .take(FEATURED_LIMIT)

    fun nonEmptyRails(rails: List<HomeRail>): List<HomeRail> = rails.filter { it.items.isNotEmpty() }

    fun continueRail(content: HomeContent): HomeRail? = content.rails.firstOrNull { it.id == CONTINUE_RAIL_ID }

    fun initialHero(content: HomeContent): MediaItem? =
        continueRail(content)?.items?.firstOrNull()
            ?: content.featured.firstOrNull()
            ?: content.rails.firstNotNullOfOrNull { it.items.firstOrNull() }

    fun selectionId(item: MediaItem): String = item.episodeSeriesId() ?: item.id

    fun cardImageUrl(
        railId: String,
        item: MediaItem,
    ): String? =
        if (railId == CONTINUE_RAIL_ID && item.isEpisode()) {
            item.seriesPrimaryImageUrl ?: item.primaryImageUrl ?: item.backdropImageUrl
        } else {
            item.primaryImageUrl ?: item.backdropImageUrl
        }

    private fun MediaItem.episodeSeriesId(): String? = seriesId.takeIf { isEpisode() && !it.isNullOrBlank() }

    private fun MediaItem.isEpisode(): Boolean = type == "Episode"
}
