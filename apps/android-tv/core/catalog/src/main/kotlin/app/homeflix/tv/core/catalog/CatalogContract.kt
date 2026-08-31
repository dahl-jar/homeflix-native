package app.homeflix.tv.core.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class MediaPage(
    val items: List<MediaItem>,
    val totalRecordCount: Int,
)

object CatalogContract {
    fun views(
        json: Json,
        payload: String,
    ): List<LibrarySummary> =
        json.decodeFromString<ViewsResponse>(payload).items.map { view ->
            LibrarySummary(
                id = view.id,
                name = view.name,
                collectionType = view.collectionType,
            )
        }

    fun items(
        json: Json,
        baseUrl: String,
        payload: String,
    ): List<MediaItem> = json.decodeFromString<ItemsResponse>(payload).items.map { item -> mapItem(baseUrl, item) }

    fun item(
        json: Json,
        baseUrl: String,
        payload: String,
    ): MediaItem = mapItem(baseUrl, json.decodeFromString<ItemDto>(payload))

    fun recentItems(
        json: Json,
        baseUrl: String,
        payload: String,
    ): List<MediaItem> =
        json
            .decodeFromString<ItemsResponse>(payload)
            .items
            .filterLocalPaths()
            .map { item -> mapItem(baseUrl, item) }

    fun latestItems(
        json: Json,
        baseUrl: String,
        payload: String,
    ): List<MediaItem> =
        json
            .decodeFromString<List<ItemDto>>(payload)
            .filterLocalPaths()
            .map { item -> mapItem(baseUrl, item) }

    fun page(
        json: Json,
        baseUrl: String,
        payload: String,
    ): MediaPage {
        val response = json.decodeFromString<ItemsResponse>(payload)
        return MediaPage(
            items = response.items.map { item -> mapItem(baseUrl, item) },
            totalRecordCount = response.totalRecordCount,
        )
    }

    private fun List<ItemDto>.filterLocalPaths(): List<ItemDto> = filter { item -> item.hasLocalPath() }

    private fun ItemDto.hasLocalPath(): Boolean = path?.startsWith("http") != true

    private fun mapItem(
        baseUrl: String,
        item: ItemDto,
    ): MediaItem {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val seriesPrimaryUrl =
            if (item.seriesId != null && item.seriesPrimaryImageTag != null) {
                imageUrl(normalizedBaseUrl, item.seriesId, "Primary", item.seriesPrimaryImageTag, POSTER_WIDTH)
            } else {
                null
            }
        val primaryUrl =
            item.imageTags[PRIMARY_IMAGE]?.let { primaryTag ->
                imageUrl(normalizedBaseUrl, item.id, "Primary", primaryTag, POSTER_WIDTH)
            } ?: seriesPrimaryUrl
        val backdropUrl =
            item.backdropImageTags.firstOrNull()?.let { backdropTag ->
                imageUrl(normalizedBaseUrl, item.id, "Backdrop/0", backdropTag, BACKDROP_WIDTH)
            } ?: primaryUrl

        return MediaItem(
            id = item.id,
            name = item.name,
            type = item.type,
            seriesId = item.seriesId,
            year = item.productionYear,
            overview = item.overview,
            genres = item.genres,
            primaryImageUrl = primaryUrl,
            backdropImageUrl = backdropUrl,
            playedPercentage = item.userData?.playedPercentage,
            seriesName = item.seriesName,
            indexNumber = item.indexNumber,
            parentIndexNumber = item.parentIndexNumber,
            runTimeTicks = item.runTimeTicks,
            playbackPositionTicks = item.userData?.playbackPositionTicks,
            officialRating = item.officialRating,
            communityRating = item.communityRating,
            seriesPrimaryImageUrl = seriesPrimaryUrl,
        )
    }

    private fun imageUrl(
        baseUrl: String,
        itemId: String,
        imageType: String,
        tag: String,
        maxWidth: Int,
    ): String = "$baseUrl/Items/$itemId/Images/$imageType?tag=$tag&maxWidth=$maxWidth&quality=$IMAGE_QUALITY"

    @Serializable
    private data class ItemsResponse(
        @SerialName("Items") val items: List<ItemDto> = emptyList(),
        @SerialName("TotalRecordCount") val totalRecordCount: Int = 0,
    )

    @Serializable
    private data class ViewsResponse(
        @SerialName("Items") val items: List<ViewDto> = emptyList(),
    )

    @Serializable
    private data class ViewDto(
        @SerialName("Id") val id: String,
        @SerialName("Name") val name: String,
        @SerialName("CollectionType") val collectionType: String? = null,
    )

    @Serializable
    private data class ItemDto(
        @SerialName("Id") val id: String,
        @SerialName("Name") val name: String,
        @SerialName("Type") val type: String = "",
        @SerialName("SeriesId") val seriesId: String? = null,
        @SerialName("SeriesName") val seriesName: String? = null,
        @SerialName("IndexNumber") val indexNumber: Int? = null,
        @SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
        @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
        @SerialName("OfficialRating") val officialRating: String? = null,
        @SerialName("CommunityRating") val communityRating: Float? = null,
        @SerialName("ProductionYear") val productionYear: Int? = null,
        @SerialName("Overview") val overview: String? = null,
        @SerialName("Genres") val genres: List<String> = emptyList(),
        @SerialName("ImageTags") val imageTags: Map<String, String> = emptyMap(),
        @SerialName("BackdropImageTags") val backdropImageTags: List<String> = emptyList(),
        @SerialName("SeriesPrimaryImageTag") val seriesPrimaryImageTag: String? = null,
        @SerialName("Path") val path: String? = null,
        @SerialName("UserData") val userData: UserDataDto? = null,
    )

    @Serializable
    private data class UserDataDto(
        @SerialName("PlayedPercentage") val playedPercentage: Float? = null,
        @SerialName("PlaybackPositionTicks") val playbackPositionTicks: Long? = null,
    )

    private const val POSTER_WIDTH = 440
    private const val BACKDROP_WIDTH = 1_280
    private const val IMAGE_QUALITY = 90
    private const val PRIMARY_IMAGE = "Primary"
}
