package app.homeflix.tv.feature.player

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaybackNegotiatorTest {
    private val gateway = FakePlaybackInfoGateway()
    private val watcherFactory = RecordingWatcherFactory()

    @Test
    fun `should resolve release and build direct source`() =
        runTest {
            val events = mutableListOf<PipelineEvent>()

            val accepted = negotiator().negotiate(request(), events::add)

            assertEquals("source-1", accepted.released.mediaSourceId)
            assertEquals(PlayMethod.DIRECT_PLAY, accepted.released.playMethod)
            assertEquals("session-1", accepted.released.playSessionId)
            assertTrue(accepted.videoSource.url.contains("/Videos/item-1/stream"))
            assertEquals(2, accepted.sourceCount)
            assertEquals(12.0, accepted.startSeconds)
            assertTrue(events.contains(PipelineEvent.ResolutionStarted))
            assertTrue(events.contains(PipelineEvent.ReleaseCompleted))
            assertTrue(watcherFactory.stopped)
            val release = gateway.releaseRequests.single()
            assertEquals("handle-1", release.pipelineHandle)
            assertEquals("decision-1", release.pipelineDecision)
            assertEquals(1, release.audioStreamIndex)
            assertEquals(-1, release.subtitleStreamIndex)
        }

    @Test
    fun `should pass exclusions and override to resolve`() =
        runTest {
            negotiator().negotiate(
                request(
                    excludedSourceIds = setOf("bad-1", "bad-2"),
                    trackOverride =
                        TrackOverride(mediaSourceId = "source-1", audioStreamIndex = 2, subtitleStreamIndex = 4),
                ),
            ) {}

            val resolve = gateway.resolveRequests.single()
            assertEquals(setOf("bad-1", "bad-2"), resolve.rejectedSourceIds)
            assertEquals("source-1", resolve.preferredMediaSourceId)
            assertEquals(
                TrackOverride(mediaSourceId = "source-1", audioStreamIndex = 2, subtitleStreamIndex = 4),
                resolve.trackOverride,
            )
        }

    @Test
    fun `should fail without compatible source`() =
        runTest {
            gateway.resolveResult = playbackInfo(sources = emptyList())

            val failure = runCatching { negotiator().negotiate(request()) {} }.exceptionOrNull()

            assertTrue(failure is NoCompatibleSourceException)
        }

    @Test
    fun `should reject incomplete resolution`() =
        runTest {
            gateway.resolveResult = playbackInfo().copy(pipelineHandle = null)

            val failure = runCatching { negotiator().negotiate(request()) {} }.exceptionOrNull()

            assertTrue(failure is IllegalStateException)
        }

    @Test
    fun `should stop watcher when resolve throws`() =
        runTest {
            gateway.resolveFailure = IllegalStateException("resolve exploded")

            val failure = runCatching { negotiator().negotiate(request()) {} }.exceptionOrNull()

            assertTrue(failure is IllegalStateException)
            assertTrue(watcherFactory.stopped)
        }

    private fun negotiator(): PlaybackNegotiator =
        PlaybackNegotiator(
            gateway = gateway,
            deviceProfile = buildJsonObject {},
            baseUrl = "http://server.test:8096",
            watcherFactory = watcherFactory,
            createPipelineId = { "native-abc" },
        )

    private fun request(
        excludedSourceIds: Set<String> = emptySet(),
        trackOverride: TrackOverride? = null,
    ): NegotiationRequest =
        NegotiationRequest(
            item = item(),
            userId = "user-1",
            startTimeTicks = 120_000_000,
            excludedSourceIds = excludedSourceIds,
            preferredMediaSourceId = null,
            trackOverride = trackOverride,
            pipeline =
                PlaybackPipelineIds(itemId = "item-1", itemName = "Item", pipelineId = "native-abc", now = { 0L }),
        )

    private fun item(): PlayableItem =
        PlayableItem(
            id = "item-1",
            name = "Item",
            type = "Movie",
            seriesId = null,
            seriesName = null,
            indexNumber = null,
            parentIndexNumber = null,
            isMissing = false,
            resumePositionTicks = 0,
            backdropUrl = null,
        )
}

private fun playbackInfo(sources: List<MediaSourceDto> = listOf(mediaSource())): PlaybackInfoResult =
    PlaybackInfoResult(
        playSessionId = "session-1",
        errorCode = null,
        mediaSources = sources,
        pipelineHandle = "handle-1",
        pipelineDecision = "decision-1",
        pipelineAudioStreamIndex = 1,
        pipelineSubtitleStreamIndex = -1,
        pipelineSourceCount = 2,
        videoDelivery = "direct",
        audioDelivery = "direct",
        sourceWidth = 3840,
        sourceHeight = 2160,
    )

private fun mediaSource(): MediaSourceDto =
    MediaSourceDto(
        id = "source-1",
        name = "Remux",
        supportsDirectPlay = true,
        supportsDirectStream = false,
        supportsTranscoding = true,
        transcodingUrl = null,
        transcodingSubProtocol = null,
        mediaStreams = emptyList(),
    )

private class FakePlaybackInfoGateway : PlaybackInfoGateway {
    var resolveResult: PlaybackInfoResult = playbackInfo()
    var resolveFailure: Exception? = null
    val resolveRequests = mutableListOf<ResolveRequest>()
    val releaseRequests = mutableListOf<ReleaseRequest>()

    override suspend fun resolveAttempt(request: ResolveRequest): PlaybackInfoResult {
        resolveRequests.add(request)
        resolveFailure?.let { throw it }
        return resolveResult
    }

    override suspend fun releaseSource(request: ReleaseRequest): PlaybackInfoResult {
        releaseRequests.add(request)
        return resolveResult
    }
}

private class RecordingWatcherFactory : ProgressWatcherFactory {
    var stopped = false

    override fun watch(
        pipelineId: String,
        attemptId: String,
        onEvent: (PipelineEvent) -> Unit,
    ): ProgressWatchHandle =
        object : ProgressWatchHandle {
            override suspend fun stop() {
                stopped = true
            }
        }
}
