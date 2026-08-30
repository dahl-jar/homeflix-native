package app.homeflix.tv.feature.library

import app.homeflix.tv.core.catalog.CatalogContract
import app.homeflix.tv.core.catalog.LibrarySummary
import app.homeflix.tv.core.catalog.MediaPage
import app.homeflix.tv.core.network.JsonApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val LIBRARY_ITEM_TYPES = "Movie,Series"

class LibraryApi(
    private val baseUrl: String,
    private val client: JsonApiClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : LibraryGateway {
    override suspend fun fetchLibraries(userId: String): List<LibrarySummary> =
        CatalogContract.views(
            json,
            client.get(
                path = "/UserViews",
                query = mapOf("userId" to userId),
            ),
        )

    override suspend fun fetchPage(
        userId: String,
        request: LibraryPageRequest,
    ): MediaPage =
        CatalogContract.page(
            json = json,
            baseUrl = baseUrl,
            payload =
                client.get(
                    path = "/Items",
                    query =
                        mapOf(
                            "userId" to userId,
                            "parentId" to request.libraryId,
                            "recursive" to "true",
                            "includeItemTypes" to LIBRARY_ITEM_TYPES,
                            "fields" to "PrimaryImageAspectRatio",
                            "startIndex" to request.startIndex.toString(),
                            "limit" to request.limit.toString(),
                        ) + LibraryFilters.buildLibraryQuery(request.selection),
                ),
        )

    override suspend fun fetchFilterOptions(
        userId: String,
        libraryId: String,
    ): LibraryFilterOptions {
        val response =
            json.decodeFromString<FiltersResponse>(
                client.get(
                    path = "/Items/Filters",
                    query =
                        mapOf(
                            "userId" to userId,
                            "parentId" to libraryId,
                            "includeItemTypes" to LIBRARY_ITEM_TYPES,
                        ),
                ),
            )
        return LibraryFilterOptions(
            genres = response.genres,
            decades = LibraryFilters.decadesFromYears(response.years),
        )
    }

    @Serializable
    private data class FiltersResponse(
        @SerialName("Genres") val genres: List<String> = emptyList(),
        @SerialName("Years") val years: List<Int> = emptyList(),
    )
}
