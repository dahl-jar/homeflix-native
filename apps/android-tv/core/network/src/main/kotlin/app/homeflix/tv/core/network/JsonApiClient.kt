package app.homeflix.tv.core.network

interface JsonApiClient {
    suspend fun get(path: String): String

    suspend fun get(
        path: String,
        query: Map<String, String>,
    ): String =
        if (query.isEmpty()) {
            get(path)
        } else {
            error("Query parameters are not supported by this client")
        }

    suspend fun post(
        path: String,
        body: String,
    ): String
}
