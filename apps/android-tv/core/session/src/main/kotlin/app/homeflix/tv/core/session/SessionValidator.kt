package app.homeflix.tv.core.session

import app.homeflix.tv.core.network.JsonApiClient
import kotlinx.coroutines.CancellationException

object SessionValidator {
    suspend fun validate(client: JsonApiClient): Boolean =
        try {
            client.get("/Users/Me")
            true
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: Exception) {
            false
        }
}
