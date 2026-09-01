package app.homeflix.tv.core.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

private val JSON_MEDIA_TYPE = "application/json".toMediaType()

class ApiException(
    val status: Int,
    val path: String,
) : IOException("request to $path failed with status $status")

class JellyfinClient(
    baseUrl: String,
    deviceId: String,
    version: String,
    token: String = "",
    private val callFactory: Call.Factory = OkHttpClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JsonApiClient {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')
    private val authorization = JellyfinIdentity.authorizationHeader(deviceId, version, token)
    val mediaRequestHeaders: Map<String, String> = mapOf("Authorization" to authorization)

    override suspend fun get(path: String): String = get(path, emptyMap())

    override suspend fun get(
        path: String,
        query: Map<String, String>,
    ): String {
        val urlBuilder = (normalizedBaseUrl + path).toHttpUrl().newBuilder()
        query.forEach { (name, value) -> urlBuilder.addQueryParameter(name, value) }
        return execute(
            request = Request.Builder().url(urlBuilder.build()).build(),
            path = path,
        )
    }

    override suspend fun post(
        path: String,
        body: String,
    ): String =
        execute(
            request =
                Request
                    .Builder()
                    .url(normalizedBaseUrl + path)
                    .post(body.toRequestBody(JSON_MEDIA_TYPE))
                    .build(),
            path = path,
        )

    private suspend fun execute(
        request: Request,
        path: String,
    ): String =
        withContext(ioDispatcher) {
            val authenticatedRequest =
                request
                    .newBuilder()
                    .header("Authorization", authorization)
                    .build()
            callFactory.newCall(authenticatedRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw ApiException(response.code, path)
                }
                response.body.string()
            }
        }
}

internal object JellyfinIdentity {
    fun authorizationHeader(
        deviceId: String,
        version: String,
        token: String,
    ): String {
        val identity =
            "MediaBrowser Client=\"Homeflix\", Device=\"Android TV\", " +
                "DeviceId=\"${identityValue(deviceId, "deviceId")}\", " +
                "Version=\"${identityValue(version, "version")}\""
        return if (token.isBlank()) identity else "$identity, Token=\"${identityValue(token, "token")}\""
    }

    private fun identityValue(
        value: String,
        field: String,
    ): String {
        val normalized = value.trim()
        require(normalized.isNotEmpty() && '"' !in normalized) {
            "$field must be a non-empty MediaBrowser identity value"
        }
        return normalized
    }
}
