package app.homeflix.tv.core.network

private val SERVER_URL_SCHEMES = listOf("http://", "https://")

fun normalizeServerUrl(input: String): String? {
    val trimmed = input.trim().trimEnd('/')
    for (scheme in SERVER_URL_SCHEMES) {
        if (trimmed.startsWith(scheme, ignoreCase = true)) {
            return trimmed
        }
    }
    return null
}
