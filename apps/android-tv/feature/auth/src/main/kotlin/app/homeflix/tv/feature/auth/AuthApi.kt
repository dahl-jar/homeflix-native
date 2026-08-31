package app.homeflix.tv.feature.auth

import app.homeflix.tv.core.network.JsonApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PUBLIC_USERS_PATH = "/Users/Public"
private const val AUTHENTICATE_PATH = "/Users/AuthenticateByName"
private const val USER_IMAGE_QUALITY = 90

class AuthApi(
    private val baseUrl: String,
    private val client: JsonApiClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AuthGateway {
    override suspend fun fetchPublicProfiles(): List<AuthProfile> =
        AuthContract.decodeProfiles(
            json = json,
            baseUrl = baseUrl,
            response = client.get(PUBLIC_USERS_PATH),
        )

    override suspend fun authenticate(
        username: String,
        password: String,
    ): AuthenticatedUser {
        val request = AuthContract.authenticationRequest(json, username, password)
        return AuthContract.decodeAuthenticatedUser(
            json = json,
            response = client.post(AUTHENTICATE_PATH, request),
        )
    }
}

interface AuthGateway {
    suspend fun fetchPublicProfiles(): List<AuthProfile>

    suspend fun authenticate(
        username: String,
        password: String,
    ): AuthenticatedUser
}

data class AuthenticatedUser(
    val accessToken: String,
    val userId: String,
    val userName: String,
    val primaryImageTag: String? = null,
)

internal object AuthContract {
    fun decodeProfiles(
        json: Json,
        baseUrl: String,
        response: String,
    ): List<AuthProfile> =
        json.decodeFromString<List<PublicUserDto>>(response).map { user ->
            AuthProfile(
                id = user.id,
                name = user.name,
                hasPassword = user.hasPassword,
                avatarUrl = user.primaryImageTag?.let { userImageUrl(baseUrl, user.id, it) },
            )
        }

    fun authenticationRequest(
        json: Json,
        username: String,
        password: String,
    ): String = json.encodeToString(AuthenticationRequest(username, password))

    fun decodeAuthenticatedUser(
        json: Json,
        response: String,
    ): AuthenticatedUser {
        val authentication = json.decodeFromString<AuthenticationResponse>(response)
        return AuthenticatedUser(
            accessToken = authentication.accessToken,
            userId = authentication.user.id,
            userName = authentication.user.name,
            primaryImageTag = authentication.user.primaryImageTag,
        )
    }

    private fun userImageUrl(
        baseUrl: String,
        userId: String,
        imageTag: String,
    ): String = "${baseUrl.trimEnd('/')}/Users/$userId/Images/Primary?tag=$imageTag&quality=$USER_IMAGE_QUALITY"
}

@Serializable
private data class PublicUserDto(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("HasPassword") val hasPassword: Boolean = false,
    @SerialName("PrimaryImageTag") val primaryImageTag: String? = null,
)

@Serializable
private data class AuthenticationRequest(
    @SerialName("Username") val username: String,
    @SerialName("Pw") val password: String,
)

@Serializable
private data class AuthenticationResponse(
    @SerialName("AccessToken") val accessToken: String,
    @SerialName("User") val user: AuthenticatedUserDto,
)

@Serializable
private data class AuthenticatedUserDto(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("PrimaryImageTag") val primaryImageTag: String? = null,
)
