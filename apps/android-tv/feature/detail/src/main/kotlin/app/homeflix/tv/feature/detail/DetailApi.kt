package app.homeflix.tv.feature.detail

import app.homeflix.tv.core.catalog.CatalogContract
import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.core.network.JsonApiClient
import kotlinx.serialization.json.Json

class DetailApi(
    private val baseUrl: String,
    private val client: JsonApiClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : DetailGateway {
    override suspend fun fetchDetail(
        userId: String,
        itemId: String,
    ): DetailContent =
        DetailContract.detail(
            json = json,
            baseUrl = baseUrl,
            payload = client.get("/Users/$userId/Items/$itemId"),
        )

    override suspend fun fetchSimilar(
        userId: String,
        itemId: String,
    ): List<MediaItem> =
        CatalogContract.items(
            json = json,
            baseUrl = baseUrl,
            payload =
                client.get(
                    path = "/Items/$itemId/Similar",
                    query =
                        mapOf(
                            "userId" to userId,
                            "limit" to SIMILAR_LIMIT.toString(),
                        ),
                ),
        )

    override suspend fun fetchSeasons(
        userId: String,
        seriesId: String,
    ): List<DetailSeason> =
        DetailContract.seasons(
            json = json,
            payload =
                client.get(
                    path = "/Shows/$seriesId/Seasons",
                    query = mapOf("userId" to userId),
                ),
        )

    override suspend fun fetchEpisodes(
        userId: String,
        seriesId: String,
        seasonId: String,
    ): List<MediaItem> =
        CatalogContract.items(
            json = json,
            baseUrl = baseUrl,
            payload =
                client.get(
                    path = "/Shows/$seriesId/Episodes",
                    query =
                        mapOf(
                            "userId" to userId,
                            "seasonId" to seasonId,
                            "fields" to "Overview",
                        ),
                ),
        )

    private companion object {
        const val SIMILAR_LIMIT = 12
    }
}
