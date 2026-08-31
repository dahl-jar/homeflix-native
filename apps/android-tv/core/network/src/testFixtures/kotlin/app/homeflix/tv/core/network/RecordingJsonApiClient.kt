package app.homeflix.tv.core.network

data class RecordedRequest(
    val path: String,
    val query: Map<String, String>,
)

abstract class RecordingJsonApiClient : JsonApiClient {
    val requests = mutableListOf<RecordedRequest>()

    final override suspend fun get(path: String): String = get(path, emptyMap())

    final override suspend fun get(
        path: String,
        query: Map<String, String>,
    ): String {
        requests += RecordedRequest(path, query)
        return respond(path, query)
    }

    override suspend fun post(
        path: String,
        body: String,
    ): String = error("unexpected POST")

    protected abstract fun respond(
        path: String,
        query: Map<String, String>,
    ): String
}
