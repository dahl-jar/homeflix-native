package app.homeflix.tv.feature.auth

import app.homeflix.tv.core.network.JsonApiClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AuthApiTest {
    @Test
    fun `should decode public profiles`() =
        runBlocking {
            val client = RecordingClient(getResponse = fixture("users-public.json"))
            val api = AuthApi(baseUrl = "http://server", client = client)

            val profiles = api.fetchPublicProfiles()

            assertEquals(3, profiles.size)
            assertEquals("user-one", profiles[0].id)
            assertEquals("Darrow", profiles[0].name)
            assertEquals(true, profiles[0].hasPassword)
            assertEquals(
                "http://server/Users/user-one/Images/Primary?tag=image-one&quality=90",
                profiles[0].avatarUrl,
            )
            assertNull(profiles[1].avatarUrl)
            assertEquals("user-three", profiles[2].id)
            assertEquals("Goblin", profiles[2].name)
            assertEquals(false, profiles[2].hasPassword)
            assertEquals("/Users/Public", client.getPaths.single())
        }

    @Test
    fun `should send empty password for passwordless profile`() =
        runBlocking {
            val client =
                RecordingClient(
                    postResponse =
                        """{"AccessToken":"access-token","User":{"Id":"user-three","Name":"Goblin"}}""",
                )
            val api = AuthApi(baseUrl = "http://server", client = client)

            val authenticated = api.authenticate(username = "Goblin", password = "")

            assertEquals("access-token", authenticated.accessToken)
            assertEquals("user-three", authenticated.userId)
            assertEquals("/Users/AuthenticateByName", client.postPaths.single())
            val body = Json.parseToJsonElement(client.postBodies.single()).toString()
            assertEquals("""{"Username":"Goblin","Pw":""}""", body)
        }

    @Test
    fun `should map authenticated image tag`() =
        runBlocking {
            val client =
                RecordingClient(
                    postResponse =
                        """
                        {"AccessToken":"access-token","User":{"Id":"user-one","Name":"Darrow",
                        "PrimaryImageTag":"image-one"}}
                        """.trimIndent(),
                )
            val api = AuthApi(baseUrl = "http://server", client = client)

            val authenticated = api.authenticate(username = "Darrow", password = "4321")

            assertEquals("image-one", authenticated.primaryImageTag)
        }

    private fun fixture(name: String): String = requireNotNull(javaClass.classLoader?.getResource(name)).readText()
}

private class RecordingClient(
    private val getResponse: String = "",
    private val postResponse: String = "",
) : JsonApiClient {
    val getPaths = mutableListOf<String>()
    val postPaths = mutableListOf<String>()
    val postBodies = mutableListOf<String>()

    override suspend fun get(path: String): String {
        getPaths += path
        return getResponse
    }

    override suspend fun post(
        path: String,
        body: String,
    ): String {
        postPaths += path
        postBodies += body
        return postResponse
    }
}
