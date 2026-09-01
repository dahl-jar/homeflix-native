package app.homeflix.tv.feature.player

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackRuntimeTest {
    @Test
    fun `should load negotiated source and expose track catalog`() =
        runTest {
            val fixture = RuntimeFixture(this)

            fixture.runtime.start()

            val snapshot = fixture.runtime.snapshot.value
            assertEquals(PlaybackStatus.LOADING, snapshot.status)
            assertEquals(
                listOf("English · 5.1", "Norwegian · Stereo"),
                snapshot.tracks.audioTracks.map(PlaybackTrack::label),
            )
            assertEquals(1, snapshot.tracks.selectedAudioTrack?.streamIndex)
            assertEquals(
                "player",
                snapshot.pipeline.stages
                    .single()
                    .id,
            )
            assertEquals(1, fixture.bindings.size)
            assertEquals(12.0, fixture.bindings.single().loadedStartSeconds)
        }

    @Test
    fun `should start playback once and report start`() =
        runTest {
            val fixture = RuntimeFixture(this)
            fixture.runtime.start()
            val binding = fixture.bindings.single()

            binding.callbacks.onPlayingChange(true)
            binding.callbacks.onPlayingChange(true)
            runCurrent()

            val snapshot = fixture.runtime.snapshot.value
            assertEquals(PlaybackStatus.PLAYING, snapshot.status)
            assertFalse(snapshot.pipeline.visible)
            assertEquals(listOf("start"), fixture.sessionGateway.calls.map(String::toString))
        }

    @Test
    fun `should emit source parity events after negotiation`() =
        runTest {
            val fixture = RuntimeFixture(this)

            fixture.runtime.start()
            runCurrent()

            val events =
                fixture.telemetryEvents.map { event ->
                    event.getValue("event").jsonPrimitive.content
                }
            val parityEvents = listOf("sources_loaded", "source_selected", "tracks_resolved", "source_accepted")
            assertTrue(events.containsAll(parityEvents))
        }

    @Test
    fun `should report pause time in seconds`() =
        runTest {
            val fixture = RuntimeFixture(this)
            fixture.runtime.start()
            val binding = fixture.bindings.single()
            binding.callbacks.onPlayingChange(true)
            binding.callbacks.onTimeUpdate(
                positionSeconds = 25.0,
                durationSeconds = 100.0,
                bufferedSeconds = 30.0,
                playbackAdvanced = true,
            )
            binding.callbacks.onPlayingChange(false)
            runCurrent()

            val pause =
                fixture.telemetryEvents.single {
                    it["event"]?.jsonPrimitive?.content == "playback_paused"
                }
            val positionSeconds =
                pause
                    .getValue("videoCurrentTime")
                    .jsonPrimitive.content
                    .toDouble()
            assertEquals(25.0, positionSeconds)
        }

    @Test
    fun `should report buffering duration`() =
        runTest {
            val fixture = RuntimeFixture(this)
            fixture.runtime.start()
            val callbacks = fixture.bindings.single().callbacks
            fixture.nowMs = 1_000
            callbacks.onPlaybackStateChange(PlaybackEngineState.BUFFERING, playWhenReady = true)
            fixture.nowMs = 6_000
            callbacks.onPlaybackStateChange(PlaybackEngineState.READY, playWhenReady = true)
            runCurrent()

            val started =
                fixture.telemetryEvents.single {
                    it["event"]?.jsonPrimitive?.content == "buffering_started"
                }
            val recovered =
                fixture.telemetryEvents.single {
                    it["event"]?.jsonPrimitive?.content == "buffering_recovered"
                }
            val positionSeconds =
                started
                    .getValue("videoCurrentTime")
                    .jsonPrimitive.content
                    .toDouble()
            val bufferingDurationMs = recovered.getValue("bufferingDurationMs").jsonPrimitive.int
            assertEquals(12.0, positionSeconds)
            assertEquals(5_000, bufferingDurationMs)
        }

    @Test
    fun `should sample memory once per bounded interval`() =
        runTest {
            val fixture =
                RuntimeFixture(
                    scope = this,
                    memoryUsage = {
                        PlaybackMemoryUsage(
                            heapUsedBytes = 90,
                            heapMaxBytes = 192,
                            nativeHeapAllocatedBytes = 20,
                            totalPssKb = 140,
                        )
                    },
                )
            fixture.runtime.start()

            advanceTimeBy(10_000)
            runCurrent()

            val snapshots =
                fixture.telemetryEvents.filter {
                    it["event"]?.jsonPrimitive?.content == "memory_snapshot"
                }
            assertEquals(1, snapshots.size)
            val heapUsedBytes =
                snapshots
                    .single()
                    .getValue("heapUsedBytes")
                    .jsonPrimitive.int
            assertEquals(90, heapUsedBytes)
            fixture.runtime.stop()
        }

    @Test
    fun `should recover on player error excluding failed source`() =
        runTest {
            val fixture = RuntimeFixture(this)
            fixture.runtime.start()
            fixture.bindings
                .single()
                .callbacks
                .onPlayingChange(true)
            runCurrent()

            fixture.bindings
                .single()
                .callbacks
                .onError(
                    PlayerErrorDetails(
                        reason = "ERROR_CODE_DECODING_FAILED",
                        telemetry =
                            mapOf(
                                "errorType" to "MediaCodecVideoDecoderException",
                                "errorCode" to 4_003,
                                "errorName" to "ERROR_CODE_DECODING_FAILED",
                                "errorMessage" to
                                    "renderer=MediaCodecVideoRenderer; decoder=c2.android.hevc.decoder; " +
                                    "diagnostic=android.media.MediaCodec.error_neg_2147483648",
                            ),
                    ),
                )
            runCurrent()

            assertEquals(2, fixture.negotiations.size)
            assertEquals(setOf("source-1"), fixture.negotiations[1].excludedSourceIds)
            assertTrue(fixture.bindings.first().disposed)
            assertEquals(2, fixture.bindings.size)
            assertEquals(2, fixture.runtime.snapshot.value.pipeline.attempt)
            val failureEvent =
                fixture.telemetryEvents.single { event ->
                    event["event"]?.jsonPrimitive?.content == "player_failed"
                }
            assertEquals("MediaCodecVideoDecoderException", failureEvent.getValue("errorType").jsonPrimitive.content)
            assertEquals(4_003, failureEvent.getValue("errorCode").jsonPrimitive.int)
            assertEquals("ERROR_CODE_DECODING_FAILED", failureEvent.getValue("errorName").jsonPrimitive.content)
            assertTrue(
                failureEvent
                    .getValue("errorMessage")
                    .jsonPrimitive.content
                    .contains("c2.android.hevc.decoder"),
            )
            assertEquals(0, failureEvent.getValue("elapsedMs").jsonPrimitive.int)
        }

    @Test
    fun `should recover from backward reset using last confirmed position`() =
        runTest {
            val fixture = RuntimeFixture(this)
            val binding =
                fixture.startPlayingAt(
                    positionSeconds = 600.0,
                    durationSeconds = 1_000.0,
                    bufferedSeconds = 650.0,
                )
            binding.callbacks.onTimeUpdate(
                positionSeconds = 0.0,
                durationSeconds = 0.0,
                bufferedSeconds = 0.0,
                playbackAdvanced = false,
            )
            runCurrent()

            fixture.assertRecoveredAt(startTimeTicks = 6_000_000_000L)
        }

    @Test
    fun `should recover when startup remains buffering`() =
        runTest {
            val fixture = RuntimeFixture(this)
            fixture.runtime.start()
            val binding = fixture.bindings.single()
            binding.callbacks.onPlaybackStateChange(PlaybackEngineState.BUFFERING, playWhenReady = true)
            fixture.nowMs = 30_000
            binding.callbacks.onTimeUpdate(
                positionSeconds = 12.0,
                durationSeconds = 1_000.0,
                bufferedSeconds = 12.0,
                playbackAdvanced = false,
            )
            runCurrent()

            assertEquals(2, fixture.negotiations.size)
            assertEquals(setOf("source-1"), fixture.negotiations.last().excludedSourceIds)
        }

    @Test
    fun `should recover when source ends before expected runtime`() =
        runTest {
            val fixture = RuntimeFixture(this, runTimeTicks = 10_000_000_000L)
            val binding =
                fixture.startPlayingAt(
                    positionSeconds = 600.0,
                    durationSeconds = 1_000.0,
                    bufferedSeconds = 600.0,
                )
            binding.callbacks.onEnded()
            runCurrent()

            fixture.assertRecoveredAt(startTimeTicks = 6_000_000_000L)
        }

    @Test
    fun `should fail when recovery has no source left`() =
        runTest {
            val fixture = RuntimeFixture(this)
            fixture.runtime.start()
            fixture.bindings
                .single()
                .callbacks
                .onPlayingChange(true)
            runCurrent()
            fixture.failNextNegotiation = true

            fixture.bindings
                .single()
                .callbacks
                .onError(PlayerErrorDetails(reason = "decoder failed"))
            runCurrent()

            val snapshot = fixture.runtime.snapshot.value
            assertEquals(PlaybackStatus.FAILED, snapshot.status)
            assertEquals("no compatible playback source", snapshot.reason)
        }

    @Test
    fun `should switch audio natively during direct play`() =
        runTest {
            val fixture = RuntimeFixture(this)
            fixture.runtime.start()
            val binding = fixture.bindings.single()

            fixture.runtime.selectAudioTrack(
                fixture.runtime.snapshot.value.tracks.audioTracks
                    .last(),
            )

            assertEquals(1, fixture.negotiations.size)
            assertEquals(1, binding.nativeAudioSelections.size)
            assertEquals(
                2,
                fixture.runtime.snapshot.value.tracks.selectedAudioTrack
                    ?.streamIndex,
            )
        }

    @Test
    fun `should renegotiate subtitle override when transcoding`() =
        runTest {
            val fixture = RuntimeFixture(this, playMethodOverride = PlayMethod.TRANSCODE)
            fixture.runtime.start()
            runCurrent()

            fixture.runtime.selectSubtitleTrack(
                fixture.runtime.snapshot.value.tracks.subtitleTracks
                    .single(),
            )
            runCurrent()

            assertEquals(2, fixture.negotiations.size)
            val override = fixture.negotiations[1].trackOverride
            assertEquals(3, override?.subtitleStreamIndex)
            assertEquals(1, override?.audioStreamIndex)
        }

    @Test
    fun `should keep pipeline hidden during track renegotiation`() =
        runTest {
            val fixture = RuntimeFixture(this, playMethodOverride = PlayMethod.TRANSCODE)
            fixture.runtime.start()
            fixture.bindings
                .single()
                .callbacks
                .onPlayingChange(true)
            runCurrent()

            fixture.runtime.selectSubtitleTrack(
                fixture.runtime.snapshot.value.tracks.subtitleTracks
                    .single(),
            )
            runCurrent()

            assertEquals(2, fixture.negotiations.size)
            assertFalse(fixture.runtime.snapshot.value.pipeline.visible)
        }

    @Test
    fun `should stop and dispose once`() =
        runTest {
            val fixture = RuntimeFixture(this)
            fixture.runtime.start()
            fixture.bindings
                .single()
                .callbacks
                .onPlayingChange(true)
            runCurrent()

            fixture.runtime.stop()
            fixture.runtime.stop()
            runCurrent()

            assertEquals(PlaybackStatus.ENDED, fixture.runtime.snapshot.value.status)
            assertTrue(fixture.bindings.single().disposed)
            assertEquals(listOf("start", "stop"), fixture.sessionGateway.calls)
            assertNull(fixture.runtime.snapshot.value.reason)
        }
}

private class RuntimeFixture(
    scope: kotlinx.coroutines.CoroutineScope,
    playMethodOverride: PlayMethod = PlayMethod.DIRECT_PLAY,
    runTimeTicks: Long? = null,
    memoryUsage: (() -> PlaybackMemoryUsage)? = null,
) {
    val negotiations = mutableListOf<NegotiationRequest>()
    val bindings = mutableListOf<FakeBinding>()
    val sessionGateway = CallRecordingSessionGateway()
    val telemetryEvents = mutableListOf<kotlinx.serialization.json.JsonObject>()
    var failNextNegotiation = false
    var nowMs = 0L

    private val telemetryGateway =
        object : TelemetryGateway {
            override suspend fun logPipelineEvent(payload: kotlinx.serialization.json.JsonObject) {
                telemetryEvents.add(payload)
            }
        }

    private val pipeline =
        PlaybackPipelineIds(
            itemId = "item-1",
            itemName = "Item",
            pipelineId = "native-abc",
            now = { 0L },
        )

    val runtime =
        PlaybackRuntime(
            scope = scope,
            dependencies =
                PlaybackRuntimeDependencies(
                    negotiate = { request, onEvent ->
                        negotiations.add(request)
                        if (failNextNegotiation) throw NoCompatibleSourceException()
                        onEvent(PipelineEvent.ResolutionStarted)
                        onEvent(PipelineEvent.ReleaseCompleted)
                        accepted(request, playMethodOverride)
                    },
                    bindPlayer = { callbacks -> FakeBinding(callbacks).also(bindings::add) },
                    createReporter = { context ->
                        PlaybackSessionReporter(gateway = sessionGateway, context = context, now = { 0L })
                    },
                    telemetry =
                        PlaybackTelemetry(
                            gateway = telemetryGateway,
                            pipeline = pipeline,
                            scope = scope,
                        ),
                    pipeline = pipeline,
                    monitoring =
                        PlaybackRuntimeMonitoring(
                            nowMs = { nowMs },
                            memoryUsage = memoryUsage,
                        ),
                ),
            request =
                PlaybackStartRequest(
                    item =
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
                            runTimeTicks = runTimeTicks,
                        ),
                    userId = "user-1",
                    startTimeTicks = 120_000_000,
                ),
        )

    suspend fun startPlayingAt(
        positionSeconds: Double,
        durationSeconds: Double,
        bufferedSeconds: Double,
    ): FakeBinding {
        runtime.start()
        val binding = bindings.single()
        binding.callbacks.onPlayingChange(true)
        binding.callbacks.onFirstFrame()
        binding.callbacks.onTimeUpdate(
            positionSeconds = positionSeconds,
            durationSeconds = durationSeconds,
            bufferedSeconds = bufferedSeconds,
            playbackAdvanced = true,
        )
        return binding
    }

    fun assertRecoveredAt(startTimeTicks: Long) {
        assertEquals(2, negotiations.size)
        assertEquals(startTimeTicks, negotiations.last().startTimeTicks)
        assertEquals(setOf("source-1"), negotiations.last().excludedSourceIds)
    }

    private fun accepted(
        request: NegotiationRequest,
        playMethod: PlayMethod,
    ): AcceptedPlayback {
        val mediaSource =
            MediaSourceDto(
                id = "source-${negotiations.size}",
                name = "Source",
                supportsDirectPlay = playMethod == PlayMethod.DIRECT_PLAY,
                supportsDirectStream = false,
                supportsTranscoding = true,
                transcodingUrl = "/videos/item-1/main.m3u8",
                transcodingSubProtocol = "hls",
                mediaStreams = fixtureStreams(),
            )
        val released =
            ReleasedPlayback(
                itemId = "item-1",
                mediaSourceId = mediaSource.id,
                playSessionId = "session-1",
                playMethod = playMethod,
                audioStreamIndex = 1,
                subtitleStreamIndex = -1,
                pipelineId = "native-abc",
                attemptId = "native-abc-a${negotiations.size}",
                transcodingUrl = mediaSource.transcodingUrl,
                transcodingSubProtocol = mediaSource.transcodingSubProtocol,
            )
        return AcceptedPlayback(
            released = released,
            mediaSource = mediaSource,
            videoSource = videoSource("http://server", released),
            sourceCount = 3,
            startSeconds = request.startTimeTicks.toDouble() / 10_000_000L,
            videoDelivery = null,
            audioDelivery = null,
            sourceWidth = null,
            sourceHeight = null,
        )
    }
}

private fun fixtureStreams(): List<MediaStreamDto> =
    listOf(
        fixtureStream(index = 1, type = "Audio", language = "eng", channels = 6),
        fixtureStream(index = 2, type = "Audio", language = "nor", channels = 2),
        fixtureStream(index = 3, type = "Subtitle", language = "eng", isExternal = false),
    )

private fun fixtureStream(
    index: Int,
    type: String,
    language: String,
    channels: Int? = null,
    isExternal: Boolean? = null,
): MediaStreamDto =
    MediaStreamDto(
        index = index,
        type = type,
        language = language,
        displayTitle = null,
        title = null,
        channels = channels,
        isForced = false,
        isHearingImpaired = false,
        isExternal = isExternal,
    )

private class FakeBinding(
    val callbacks: PlayerCallbacks,
) : PlayerBinding {
    var loadedStartSeconds: Double? = null
    var disposed = false
    val nativeAudioSelections = mutableListOf<Int>()

    override suspend fun load(
        source: PlayerMediaSource,
        startSeconds: Double,
    ) {
        loadedStartSeconds = startSeconds
    }

    override fun play() = Unit

    override fun pause() = Unit

    override fun seekBy(seconds: Double) = Unit

    override fun seekTo(seconds: Double) = Unit

    override fun selectNativeAudioTrack(typeOrdinal: Int) {
        nativeAudioSelections.add(typeOrdinal)
    }

    override fun selectNativeSubtitleTrack(typeOrdinal: Int?) = Unit

    override fun snapshot(): PlayerSnapshot =
        PlayerSnapshot(positionSeconds = 30.0, durationSeconds = 100.0, isPaused = false)

    override fun dispose() {
        disposed = true
    }
}

private class CallRecordingSessionGateway : SessionReportGateway {
    val calls = mutableListOf<String>()

    override suspend fun reportStart(
        context: SessionContext,
        snapshot: SessionSnapshot,
    ) {
        calls.add("start")
    }

    override suspend fun reportProgress(
        context: SessionContext,
        snapshot: SessionSnapshot,
    ) {
        calls.add("progress")
    }

    override suspend fun reportStop(
        context: SessionContext,
        snapshot: SessionSnapshot,
    ) {
        calls.add("stop")
    }
}
