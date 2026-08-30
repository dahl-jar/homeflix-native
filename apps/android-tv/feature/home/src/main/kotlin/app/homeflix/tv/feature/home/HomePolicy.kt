package app.homeflix.tv.feature.home

object HomePolicy {
    const val CONTINUE_RAIL_ID = "continue"

    private const val FEATURED_LIMIT = 8

    fun rankFeatured(
        recommendations: List<HomeRecommendation>,
        resolvedItems: Map<String, HomeMediaItem>,
    ): List<HomeMediaItem> =
        recommendations
            .sortedBy(HomeRecommendation::rank)
            .mapNotNull { resolvedItems[it.itemId] }
            .distinctBy(HomeMediaItem::id)
            .take(FEATURED_LIMIT)

    fun nonEmptyRails(rails: List<HomeRail>): List<HomeRail> = rails.filter { it.items.isNotEmpty() }

    fun continueRail(content: HomeContent): HomeRail? = content.rails.firstOrNull { it.id == CONTINUE_RAIL_ID }

    fun initialHero(content: HomeContent): HomeMediaItem? =
        continueRail(content)?.items?.firstOrNull()
            ?: content.featured.firstOrNull()
            ?: content.rails.firstNotNullOfOrNull { it.items.firstOrNull() }

    fun selectionId(item: HomeMediaItem): String = item.episodeSeriesId() ?: item.id

    private fun HomeMediaItem.episodeSeriesId(): String? = seriesId.takeIf { isEpisode() && !it.isNullOrBlank() }

    private fun HomeMediaItem.isEpisode(): Boolean = type == "Episode"
}
