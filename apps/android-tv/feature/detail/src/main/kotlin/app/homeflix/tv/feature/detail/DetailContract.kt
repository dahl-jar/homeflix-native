package app.homeflix.tv.feature.detail

import app.homeflix.tv.core.catalog.CatalogContract
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object DetailContract {
    fun detail(
        json: Json,
        baseUrl: String,
        payload: String,
    ): DetailContent {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val cast =
            json
                .decodeFromString<PeopleDto>(payload)
                .people
                .filter { person -> person.type == ACTOR_TYPE }
                .take(CAST_LIMIT)
                .map { person ->
                    CastMember(
                        id = person.id,
                        name = person.name,
                        imageUrl =
                            person.primaryImageTag?.let { tag ->
                                "$normalizedBaseUrl/Items/${person.id}/Images/Primary" +
                                    "?tag=$tag&maxWidth=$CAST_IMAGE_WIDTH&quality=$IMAGE_QUALITY"
                            },
                    )
                }
        return DetailContent(
            item = CatalogContract.item(json, baseUrl, payload),
            cast = cast,
        )
    }

    fun seasons(
        json: Json,
        payload: String,
    ): List<DetailSeason> =
        json.decodeFromString<SeasonsResponse>(payload).items.map { season ->
            DetailSeason(
                id = season.id,
                name = season.name,
                indexNumber = season.indexNumber,
            )
        }

    @Serializable
    private data class PeopleDto(
        @SerialName("People") val people: List<PersonDto> = emptyList(),
    )

    @Serializable
    private data class PersonDto(
        @SerialName("Id") val id: String,
        @SerialName("Name") val name: String,
        @SerialName("Type") val type: String = "",
        @SerialName("PrimaryImageTag") val primaryImageTag: String? = null,
    )

    @Serializable
    private data class SeasonsResponse(
        @SerialName("Items") val items: List<SeasonDto> = emptyList(),
    )

    @Serializable
    private data class SeasonDto(
        @SerialName("Id") val id: String,
        @SerialName("Name") val name: String,
        @SerialName("IndexNumber") val indexNumber: Int? = null,
    )

    private const val ACTOR_TYPE = "Actor"
    private const val CAST_LIMIT = 12
    private const val CAST_IMAGE_WIDTH = 200
    private const val IMAGE_QUALITY = 90
}
