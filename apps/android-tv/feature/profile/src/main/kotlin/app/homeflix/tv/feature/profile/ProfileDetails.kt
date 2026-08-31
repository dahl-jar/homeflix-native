package app.homeflix.tv.feature.profile

data class ProfileDetails(
    val name: String,
    val avatarUrl: String?,
    val serverAddress: String,
    val appVersion: String,
)

fun profileServerAddress(serverUrl: String): String =
    serverUrl
        .removePrefix(HTTPS_SCHEME)
        .removePrefix(HTTP_SCHEME)
        .trimEnd('/')

private const val HTTPS_SCHEME = "https://"
private const val HTTP_SCHEME = "http://"
