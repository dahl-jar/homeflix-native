package app.homeflix.tv.feature.detail

import app.homeflix.tv.core.catalog.MediaItem

interface DetailGateway {
    suspend fun fetchDetail(
        userId: String,
        itemId: String,
    ): DetailContent

    suspend fun fetchSimilar(
        userId: String,
        itemId: String,
    ): List<MediaItem>

    suspend fun fetchSeasons(
        userId: String,
        seriesId: String,
    ): List<DetailSeason>

    suspend fun fetchEpisodes(
        userId: String,
        seriesId: String,
        seasonId: String,
    ): List<MediaItem>
}
