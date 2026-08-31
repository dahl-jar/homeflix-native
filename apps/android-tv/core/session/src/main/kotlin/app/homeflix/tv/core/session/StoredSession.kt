package app.homeflix.tv.core.session

import kotlinx.serialization.Serializable

@Serializable
data class StoredSession(
    val accessToken: String,
    val userId: String,
    val userName: String,
    val primaryImageTag: String? = null,
)
