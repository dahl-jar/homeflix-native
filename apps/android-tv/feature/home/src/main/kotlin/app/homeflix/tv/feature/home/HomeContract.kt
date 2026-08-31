package app.homeflix.tv.feature.home

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal object HomeContract {
    fun recommendations(
        json: Json,
        payload: String,
    ): List<HomeRecommendation> =
        json.decodeFromString<List<RecommendationDto>>(payload).map { recommendation ->
            HomeRecommendation(
                itemId = recommendation.itemId,
                rank = recommendation.rank,
            )
        }

    @Serializable
    private data class RecommendationDto(
        @SerialName("ItemId") val itemId: String,
        @SerialName("Rank") val rank: Int,
    )
}
