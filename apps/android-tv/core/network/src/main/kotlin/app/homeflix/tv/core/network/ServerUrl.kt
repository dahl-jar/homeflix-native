package app.homeflix.tv.core.network

private val SERVER_URL_SCHEMES = listOf("http://", "https://")

fun normalizeServerUrl(input: String): String? {
    val trimmed = input.trim().trimEnd('/')
    val scheme =
        SERVER_URL_SCHEMES.firstOrNull { candidate ->
            trimmed.startsWith(candidate, ignoreCase = true)
        }
    return when {
        scheme == null -> null
        trimmed.length <= scheme.length -> null
        else -> trimmed
    }
}
