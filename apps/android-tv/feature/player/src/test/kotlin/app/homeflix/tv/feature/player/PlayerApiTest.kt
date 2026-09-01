package app.homeflix.tv.feature.player

import app.homeflix.tv.core.network.JsonApiClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val PLAYBACK_INFO_RESPONSE = """
{
  "PlaySessionId": "session-1",
  "MediaSources": [{
    "Id": "source-1",
    "Name": "Remux",
    "Container": "mkv",
    "IsRemote": false,
    "SupportsDirectPlay": true,
    "SupportsTranscoding": true,
    "MediaStreams": [
      {"Index": 1, "Type": "Audio", "Language": "eng", "Channels": 6,
       "Codec": "eac3", "Profile": "Dolby Digital Plus", "BitRate": 1024000, "AudioSpatialFormat": "None"},
      {"Index": 3, "Type": "Subtitle", "Language": "eng", "IsExternal": true}
    ]
  }],
  "PlaybackPipelineHandle": "handle-1",
  "PlaybackPipelineDecision": "direct-play",
  "PlaybackPipelineAudioStreamIndex": 1,
  "PlaybackPipelineSubtitleStreamIndex": -1,
  "PlaybackPipelineSourceCount": 3,
  "PlaybackPipelineVideoDelivery": "direct",
  "PlaybackPipelineSourceWidth": 3840
}
"""

class PlayerApiTest {
    private val client = RecordingJsonApiClient()
    private val api = PlayerApi(baseUrl = "http://server.test:8096/", client = client)

    @Test
    fun `should send resolve body with pipeline and profile fields`() =
        runTest {
            client.response = PLAYBACK_INFO_RESPONSE

            val result =
                api.resolveAttempt(
                    ResolveRequest(
                        itemId = "item-1",
                        userId = "user-1",
                        startTimeTicks = 120,
                        deviceProfile = buildJsonObject { put("Name", "Homeflix Android TV") },
                        pipelineId = "native-abc",
                        attemptId = "native-abc-a1",
                        rejectedSourceIds = setOf("bad-1"),
                        preferredMediaSourceId = null,
                        trackOverride =
                            TrackOverride(mediaSourceId = "source-1", audioStreamIndex = 2, subtitleStreamIndex = 4),
                    ),
                )

            assertEquals("/Items/item-1/PlaybackInfo", client.lastPath)
            val body = Json.parseToJsonElement(client.lastBody.orEmpty()).jsonObject
            assertEquals("user-1", body.getValue("UserId").jsonPrimitive.content)
            assertEquals("true", body.getValue("PlaybackPipelineResolve").jsonPrimitive.content)
            assertEquals("false", body.getValue("EnableDirectStream").jsonPrimitive.content)
            assertEquals("native-abc", body.getValue("PlaybackPipelineId").jsonPrimitive.content)
            assertEquals("native-abc-a1", body.getValue("PlaybackAttemptId").jsonPrimitive.content)
            assertEquals(
                "bad-1",
                body
                    .getValue("PlaybackRejectedSourceIds")
                    .jsonArray
                    .single()
                    .jsonPrimitive.content,
            )
            assertEquals("true", body.getValue("PlaybackPipelineTrackOverride").jsonPrimitive.content)
            assertEquals("2", body.getValue("AudioStreamIndex").jsonPrimitive.content)
            assertEquals("4", body.getValue("SubtitleStreamIndex").jsonPrimitive.content)
            assertEquals(
                "Homeflix Android TV",
                body
                    .getValue("DeviceProfile")
                    .jsonObject
                    .getValue("Name")
                    .jsonPrimitive.content,
            )
            assertEquals("session-1", result.playSessionId)
            assertEquals("handle-1", result.pipelineHandle)
            assertEquals(3, result.pipelineSourceCount)
            assertEquals(-1, result.pipelineSubtitleStreamIndex)
            val mediaSource = result.mediaSources.single()
            val firstStream = mediaSource.mediaStreams.first()
            assertEquals("mkv", mediaSource.container)
            assertEquals(false, mediaSource.isRemote)
            assertEquals("eac3", firstStream.codec)
            assertEquals(1_024_000, firstStream.bitrate)
            assertEquals(
                2,
                mediaSource.mediaStreams.size,
            )
        }

    @Test
    fun `should send release body with accepted handle`() =
        runTest {
            client.response = PLAYBACK_INFO_RESPONSE

            api.releaseSource(
                ReleaseRequest(
                    itemId = "item-1",
                    userId = "user-1",
                    startTimeTicks = 0,
                    deviceProfile = buildJsonObject {},
                    pipelineId = "native-abc",
                    attemptId = "native-abc-a1",
                    mediaSourceId = "source-1",
                    pipelineHandle = "handle-1",
                    pipelineDecision = "direct-play",
                    audioStreamIndex = 1,
                    subtitleStreamIndex = -1,
                ),
            )

            val body = Json.parseToJsonElement(client.lastBody.orEmpty()).jsonObject
            assertEquals("true", body.getValue("PlaybackPipelineAccepted").jsonPrimitive.content)
            assertEquals("handle-1", body.getValue("PlaybackPipelineHandle").jsonPrimitive.content)
            assertEquals("direct-play", body.getValue("PlaybackPipelineDecision").jsonPrimitive.content)
            assertEquals("source-1", body.getValue("MediaSourceId").jsonPrimitive.content)
        }

    @Test
    fun `should read pipeline progress events`() =
        runTest {
            client.response =
                """{"Events": [
                    {"Sequence": 4, "StageId": "resolve", "Label": "Resolving", "Order": 200, "Status": "active"}
                ]}"""

            val events = api.pipelineProgress(pipelineId = "native-abc", attemptId = "native-abc-a1", afterSequence = 3)

            assertEquals("/Playback/PipelineProgress", client.lastPath)
            assertEquals("3", client.lastQuery["afterSequence"])
            assertEquals(4, events.single().sequence)
            assertEquals("resolve", events.single().stageId)
        }

    @Test
    fun `should report session lifecycle payloads`() =
        runTest {
            client.response = ""
            val context =
                SessionContext(
                    itemId = "item-1",
                    mediaSourceId = "source-1",
                    playSessionId = "session-1",
                    pipelineId = "native-abc",
                    attemptId = "native-abc-a1",
                    playMethod = PlayMethod.DIRECT_PLAY,
                    audioStreamIndex = 1,
                    subtitleStreamIndex = -1,
                )

            api.reportStart(context, SessionSnapshot(positionTicks = 100, isPaused = false))
            assertEquals("/Sessions/Playing", client.lastPath)

            api.reportStop(context, SessionSnapshot(positionTicks = 900, isPaused = true, failed = true))
            assertEquals("/Sessions/Playing/Stopped", client.lastPath)
            val stop = Json.parseToJsonElement(client.lastBody.orEmpty()).jsonObject
            assertEquals("DirectPlay", stop.getValue("PlayMethod").jsonPrimitive.content)
            assertEquals("900", stop.getValue("PositionTicks").jsonPrimitive.content)
            assertEquals("true", stop.getValue("Failed").jsonPrimitive.content)
        }

    @Test
    fun `should map fetched item with resume and backdrop`() =
        runTest {
            client.response = """
                {
                  "Id": "item-1",
                  "Name": "Movie",
                  "Type": "Movie",
                  "UserData": {"PlaybackPositionTicks": 5000},
                  "BackdropImageTags": ["tag-1"]
                }
            """

            val item = api.fetchItem(userId = "user-1", itemId = "item-1")

            assertEquals("false", client.lastQuery["includeMediaSources"])
            assertEquals(5000, item.resumePositionTicks)
            assertEquals(
                "http://server.test:8096/Items/item-1/Images/Backdrop/0?tag=tag-1&maxWidth=1280&quality=90",
                item.backdropUrl,
            )
        }

    @Test
    fun `should read next up episode`() =
        runTest {
            client.response =
                """{"Items": [{"Id": "e2", "Name": "Episode", "Type": "Episode", "SeriesId": "series-1"}]}"""

            val episode = api.nextUpEpisode(userId = "user-1", seriesId = "series-1")

            assertEquals("/Shows/NextUp", client.lastPath)
            assertEquals("series-1", client.lastQuery["seriesId"])
            assertEquals("e2", episode?.id)

            client.response = """{"Items": []}"""
            assertNull(api.nextUpEpisode(userId = "user-1", seriesId = "series-1"))
        }

    @Test
    fun `should read media segments`() =
        runTest {
            client.response =
                """{"Items": [{"Id": "seg-1", "Type": "Intro", "StartTicks": 0, "EndTicks": 900000000}]}"""

            val segments = api.mediaSegments("item-1")

            assertEquals("/MediaSegments/item-1", client.lastPath)
            assertEquals("Intro,Recap,Outro", client.lastQuery["includeSegmentTypes"])
            assertEquals("seg-1", segments.single().id)
        }

    @Test
    fun `should map episode still runtime and overview`() =
        runTest {
            client.response = """
                {"Items": [{
                    "Id": "e2",
                    "Name": "Episode",
                    "Type": "Episode",
                    "Overview": "Plot",
                    "RunTimeTicks": 27000000000,
                    "ImageTags": {"Primary": "still-1"},
                    "UserData": {"PlaybackPositionTicks": 13500000000}
                }]}
            """

            val episode = api.seriesEpisodes(userId = "user-1", seriesId = "series-1", itemId = "e1").single()

            assertEquals("Plot", episode.overview)
            assertEquals(27_000_000_000, episode.runTimeTicks)
            assertEquals(
                "http://server.test:8096/Items/e2/Images/Primary?tag=still-1&maxWidth=400&quality=90",
                episode.primaryImageUrl,
            )
            assertEquals("45m", episodeRuntimeText(episode.runTimeTicks))
            assertEquals(0.5f, episodeProgress(episode))
        }

    @Test
    fun `should request following episodes from current item`() =
        runTest {
            client.response = """{"Items": [{"Id": "e1", "Type": "Episode"}, {"Id": "e2", "Type": "Episode"}]}"""

            val episodes = api.followingEpisodes(userId = "user-1", seriesId = "series-1", itemId = "e1")

            assertEquals("/Shows/series-1/Episodes", client.lastPath)
            assertEquals("e1", client.lastQuery["startItemId"])
            assertTrue(episodes.map(PlayableItem::id).containsAll(listOf("e1", "e2")))
        }
}

private class RecordingJsonApiClient : JsonApiClient {
    var response: String = ""
    var lastPath: String? = null
    var lastBody: String? = null
    var lastQuery: Map<String, String> = emptyMap()

    override suspend fun get(path: String): String = get(path, emptyMap())

    override suspend fun get(
        path: String,
        query: Map<String, String>,
    ): String {
        lastPath = path
        lastQuery = query
        return response
    }

    override suspend fun post(
        path: String,
        body: String,
    ): String {
        lastPath = path
        lastBody = body
        return response
    }
}
