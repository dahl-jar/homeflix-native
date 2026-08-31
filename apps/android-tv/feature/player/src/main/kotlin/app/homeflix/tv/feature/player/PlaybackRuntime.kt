package app.homeflix.tv.feature.player

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TICKS_PER_SECOND = 10_000_000L
private const val NO_SUBTITLE_STREAM_INDEX = -1
private const val SOURCE_FAILED_MESSAGE = "failed"

data class PlaybackTrack(
    val label: String,
    val language: String?,
    val streamIndex: Int,
    val typeOrdinal: Int,
    val isExternal: Boolean,
)

data class TrackCatalog(
    val audioTracks: List<PlaybackTrack>,
    val subtitleTracks: List<PlaybackTrack>,
    val selectedAudioTrack: PlaybackTrack?,
    val selectedSubtitleTrack: PlaybackTrack?,
)

data class PlaybackSnapshot(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val positionSeconds: Double = 0.0,
    val durationSeconds: Double = 0.0,
    val bufferedSeconds: Double = 0.0,
    val tracks: TrackCatalog = TrackCatalog(emptyList(), emptyList(), null, null),
    val pipeline: PipelineProgress = PipelineProgress(),
    val reason: String? = null,
)

data class PlayerSnapshot(
    val positionSeconds: Double,
    val durationSeconds: Double,
    val isPaused: Boolean,
)

interface PlayerCallbacks {
    fun onReady(durationSeconds: Double)

    fun onPlayingChange(isPlaying: Boolean)

    fun onTimeUpdate(
        positionSeconds: Double,
        durationSeconds: Double,
        bufferedSeconds: Double,
        playbackAdvanced: Boolean,
    )

    fun onEnded()

    fun onError(error: PlayerErrorDetails)
}

interface PlayerBinding {
    suspend fun load(
        source: PlayerMediaSource,
        startSeconds: Double,
    )

    fun play()

    fun pause()

    fun seekBy(seconds: Double)

    fun seekTo(seconds: Double)

    fun selectNativeAudioTrack(typeOrdinal: Int)

    fun selectNativeSubtitleTrack(typeOrdinal: Int?)

    fun snapshot(): PlayerSnapshot

    fun dispose()
}

fun trackCatalog(
    mediaSource: MediaSourceDto,
    audioStreamIndex: Int,
    subtitleStreamIndex: Int,
): TrackCatalog {
    val audioTracks = selectableTracks(mediaSource, "Audio", TrackKind.AUDIO)
    val subtitleTracks = selectableTracks(mediaSource, "Subtitle", TrackKind.SUBTITLE)
    return TrackCatalog(
        audioTracks = audioTracks,
        subtitleTracks = subtitleTracks,
        selectedAudioTrack = audioTracks.find { it.streamIndex == audioStreamIndex },
        selectedSubtitleTrack = subtitleTracks.find { it.streamIndex == subtitleStreamIndex },
    )
}

private fun selectableTracks(
    mediaSource: MediaSourceDto,
    streamType: String,
    kind: TrackKind,
): List<PlaybackTrack> {
    val typeStreams = mediaSource.mediaStreams.filter { it.type == streamType }
    val selectable = typeStreams.filter { isSelectableTrack(it, kind) }
    val labels = playbackTrackLabels(selectable, kind)
    return selectable.mapIndexed { index, stream ->
        PlaybackTrack(
            label = labels[index],
            language = stream.language,
            streamIndex = stream.index,
            typeOrdinal = typeStreams.indexOf(stream),
            isExternal = stream.isExternal == true,
        )
    }
}

data class PlaybackStartRequest(
    val item: PlayableItem,
    val userId: String,
    val startTimeTicks: Long,
    val preferredMediaSourceId: String? = null,
)

class PlaybackRuntimeDependencies(
    val negotiate: suspend (NegotiationRequest, (PipelineEvent) -> Unit) -> AcceptedPlayback,
    val bindPlayer: (PlayerCallbacks) -> PlayerBinding,
    val createReporter: (SessionContext) -> PlaybackSessionReporter,
    val telemetry: PlaybackTelemetry,
    val pipeline: PlaybackPipelineIds,
)

@Suppress("TooManyFunctions")
class PlaybackRuntime(
    private val scope: CoroutineScope,
    private val dependencies: PlaybackRuntimeDependencies,
    private val request: PlaybackStartRequest,
) {
    private val telemetry = dependencies.telemetry

    private val state = MutableStateFlow(PlaybackSnapshot())
    val snapshot: StateFlow<PlaybackSnapshot> = state.asStateFlow()

    private var accepted: AcceptedPlayback? = null
    private var reporter: PlaybackSessionReporter? = null
    private var binding: PlayerBinding? = null
    private var closed = false
    private var recovering = false
    private var suppressPipelineEvents = false
    private var started = false
    private val rejectedSourceIds = mutableSetOf<String>()

    suspend fun start() {
        if (closed) return
        update { it.copy(status = PlaybackStatus.LOADING, reason = null) }
        try {
            loadAccepted(negotiateNext(request.startTimeTicks, trackOverride = null))
        } catch (failure: CancellationException) {
            throw failure
        } catch (expected: Exception) {
            if (closed) return
            failPlayback(expected.message ?: "playback failed")
        }
    }

    fun play() {
        binding?.play()
    }

    fun pause() {
        binding?.pause()
    }

    fun seekBy(seconds: Double) {
        binding?.seekBy(seconds)
    }

    fun seekTo(seconds: Double) {
        binding?.seekTo(seconds)
    }

    suspend fun selectAudioTrack(track: PlaybackTrack) {
        val current = accepted ?: return
        if (track.streamIndex == current.released.audioStreamIndex) return
        if (current.released.playMethod == PlayMethod.DIRECT_PLAY) {
            binding?.selectNativeAudioTrack(track.typeOrdinal)
            updateSelectedTracks(audioStreamIndex = track.streamIndex)
            telemetry.log(
                "track_override_requested",
                mapOf("audioStreamIndex" to track.streamIndex, "delivery" to "native"),
            )
        } else {
            overrideTracks(
                TrackOverride(
                    mediaSourceId = current.released.mediaSourceId,
                    audioStreamIndex = track.streamIndex,
                    subtitleStreamIndex = selectedSubtitleIndex(),
                ),
            )
        }
    }

    suspend fun selectSubtitleTrack(track: PlaybackTrack?) {
        val current = accepted ?: return
        val subtitleStreamIndex = track?.streamIndex ?: NO_SUBTITLE_STREAM_INDEX
        if (subtitleStreamIndex == selectedSubtitleIndex()) return
        val nativeSwitch =
            current.released.playMethod == PlayMethod.DIRECT_PLAY && track?.isExternal != true
        if (nativeSwitch) {
            binding?.selectNativeSubtitleTrack(track?.typeOrdinal)
            updateSelectedTracks(subtitleStreamIndex = subtitleStreamIndex)
            telemetry.log(
                "track_override_requested",
                mapOf("subtitleStreamIndex" to subtitleStreamIndex, "delivery" to "native"),
            )
        } else {
            overrideTracks(
                TrackOverride(
                    mediaSourceId = current.released.mediaSourceId,
                    audioStreamIndex = selectedAudioIndex(),
                    subtitleStreamIndex = subtitleStreamIndex,
                ),
            )
        }
    }

    suspend fun stop(status: PlaybackStatus = PlaybackStatus.ENDED) {
        if (closed) return
        closed = true
        reportStopped(failed = status == PlaybackStatus.FAILED)
        telemetry.log(
            "pipeline_stopped",
            mapOf(
                "videoCurrentTime" to state.value.positionSeconds,
                "videoEnded" to (status == PlaybackStatus.ENDED),
            ),
        )
        binding?.dispose()
        binding = null
        update { it.copy(status = status) }
    }

    private fun update(change: (PlaybackSnapshot) -> PlaybackSnapshot) {
        state.value = change(state.value)
    }

    private fun advancePipeline(event: PipelineEvent) {
        if (suppressPipelineEvents) return
        update { it.copy(pipeline = it.pipeline.transition(event)) }
    }

    private suspend fun negotiateNext(
        resumeTicks: Long,
        trackOverride: TrackOverride?,
    ): AcceptedPlayback =
        dependencies.negotiate(
            NegotiationRequest(
                item = request.item,
                userId = request.userId,
                startTimeTicks = resumeTicks,
                excludedSourceIds = rejectedSourceIds.toSet(),
                preferredMediaSourceId = request.preferredMediaSourceId,
                trackOverride = trackOverride,
                pipeline = dependencies.pipeline,
            ),
            ::advancePipeline,
        )

    private suspend fun loadAccepted(next: AcceptedPlayback) {
        accepted = next
        started = false
        reporter = dependencies.createReporter(sessionContext(next))
        val nextBinding = dependencies.bindPlayer(Callbacks())
        binding = nextBinding
        update {
            it.copy(
                status = PlaybackStatus.LOADING,
                reason = null,
                tracks =
                    trackCatalog(
                        mediaSource = next.mediaSource,
                        audioStreamIndex = next.released.audioStreamIndex,
                        subtitleStreamIndex = next.released.subtitleStreamIndex,
                    ),
            )
        }
        advancePipeline(
            PipelineEvent.StageProgress(
                stageId = PLAYER_STAGE_ID,
                label = PLAYER_STAGE_LABEL,
                order = PLAYER_STAGE_ORDER,
                status = StageStatus.ACTIVE,
            ),
        )
        nextBinding.load(next.videoSource, next.startSeconds)
    }

    private suspend fun overrideTracks(trackOverride: TrackOverride) {
        if (closed || recovering) return
        val activeBinding = binding ?: return
        recovering = true
        suppressPipelineEvents = true
        try {
            val resumeTicks = positionTicks(activeBinding)
            telemetry.log(
                "track_override_requested",
                mapOf(
                    "audioStreamIndex" to trackOverride.audioStreamIndex,
                    "subtitleStreamIndex" to trackOverride.subtitleStreamIndex,
                ),
            )
            reportStopped(failed = false)
            activeBinding.dispose()
            binding = null
            update { it.copy(status = PlaybackStatus.LOADING, reason = null) }
            try {
                loadAccepted(negotiateNext(resumeTicks, trackOverride))
            } catch (failure: CancellationException) {
                throw failure
            } catch (expected: Exception) {
                suppressPipelineEvents = false
                val reason = expected.message ?: "track override failed"
                advancePipeline(PipelineEvent.Failed(reason))
                update { it.copy(status = PlaybackStatus.FAILED, reason = reason) }
                telemetry.log("playback_failed", mapOf("reason" to "track_override_failed"))
            }
        } finally {
            suppressPipelineEvents = false
            recovering = false
        }
    }

    private suspend fun recover(error: PlayerErrorDetails) {
        if (closed || recovering) return
        val (current, failedBinding) = activePlayback() ?: return
        recovering = true
        try {
            rejectedSourceIds.add(current.released.mediaSourceId)
            telemetry.log(
                "player_failed",
                error.telemetry.ifEmpty { mapOf("errorMessage" to error.reason) },
            )
            val resumeTicks = positionTicks(failedBinding)
            advancePipeline(PipelineEvent.Failed(SOURCE_FAILED_MESSAGE))
            update { it.copy(status = PlaybackStatus.RECOVERING, reason = SOURCE_FAILED_MESSAGE) }
            reportStopped(failed = true)
            failedBinding.dispose()
            binding = null
            advancePipeline(PipelineEvent.Retry)
            val next =
                try {
                    negotiateNext(resumeTicks, trackOverride = null)
                } catch (failure: CancellationException) {
                    throw failure
                } catch (_: Exception) {
                    failPlayback("no compatible playback source")
                    telemetry.log("playback_failed", mapOf("reason" to "no_compatible_source"))
                    null
                }
            if (next != null && !closed) loadAccepted(next)
        } finally {
            recovering = false
        }
    }

    private fun failPlayback(reason: String) {
        advancePipeline(PipelineEvent.Failed(reason))
        update { it.copy(status = PlaybackStatus.FAILED, reason = reason) }
    }

    private suspend fun reportStopped(failed: Boolean) {
        val activeBinding = binding ?: return
        reporter?.stop(sessionSnapshot(activeBinding, failed = failed))
    }

    private fun sessionContext(next: AcceptedPlayback): SessionContext =
        SessionContext(
            itemId = request.item.id,
            mediaSourceId = next.released.mediaSourceId,
            playSessionId = next.released.playSessionId,
            pipelineId = next.released.pipelineId,
            attemptId = next.released.attemptId,
            playMethod = next.released.playMethod,
            audioStreamIndex = next.released.audioStreamIndex,
            subtitleStreamIndex = next.released.subtitleStreamIndex,
        )

    private fun sessionSnapshot(
        activeBinding: PlayerBinding,
        failed: Boolean = false,
    ): SessionSnapshot {
        val player = activeBinding.snapshot()
        return SessionSnapshot(
            positionTicks = (player.positionSeconds * TICKS_PER_SECOND).toLong(),
            isPaused = player.isPaused,
            failed = failed,
        )
    }

    private fun activePlayback(): Pair<AcceptedPlayback, PlayerBinding>? {
        val current = accepted
        val activeBinding = binding
        return if (current != null && activeBinding != null) current to activeBinding else null
    }

    private fun positionTicks(activeBinding: PlayerBinding): Long =
        (activeBinding.snapshot().positionSeconds * TICKS_PER_SECOND).toLong()

    private fun selectedAudioIndex(): Int =
        state.value.tracks.selectedAudioTrack
            ?.streamIndex
            ?: accepted?.released?.audioStreamIndex
            ?: NO_SUBTITLE_STREAM_INDEX

    private fun selectedSubtitleIndex(): Int =
        state.value.tracks.selectedSubtitleTrack
            ?.streamIndex
            ?: NO_SUBTITLE_STREAM_INDEX

    private fun updateSelectedTracks(
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null,
    ) {
        update { current ->
            val tracks = current.tracks
            current.copy(
                tracks =
                    tracks.copy(
                        selectedAudioTrack =
                            audioStreamIndex
                                ?.let { index -> tracks.audioTracks.find { it.streamIndex == index } }
                                ?: tracks.selectedAudioTrack,
                        selectedSubtitleTrack =
                            if (subtitleStreamIndex != null) {
                                tracks.subtitleTracks.find { it.streamIndex == subtitleStreamIndex }
                            } else {
                                tracks.selectedSubtitleTrack
                            },
                    ),
            )
        }
    }

    private fun startPlaybackOnce() {
        if (started || closed) return
        val activeBinding = binding ?: return
        started = true
        update { it.copy(status = PlaybackStatus.PLAYING) }
        advancePipeline(PipelineEvent.Playing)
        reporter?.let { activeReporter ->
            val current = sessionSnapshot(activeBinding)
            telemetry.log("playback_started", mapOf("videoCurrentTime" to state.value.positionSeconds))
            fireAndForget { activeReporter.start(current) }
        }
    }

    private fun fireAndForget(block: suspend () -> Unit) {
        scope.launch { block() }
    }

    private inner class Callbacks : PlayerCallbacks {
        override fun onReady(durationSeconds: Double) {
            if (closed) return
            update { it.copy(status = statusAfterReady(), durationSeconds = durationSeconds) }
            telemetry.log("source_ready", mapOf("videoDuration" to durationSeconds))
        }

        override fun onPlayingChange(isPlaying: Boolean) {
            if (closed || binding == null) return
            if (isPlaying) {
                startPlaybackOnce()
                if (started) update { it.copy(status = PlaybackStatus.PLAYING) }
            } else if (started) {
                update { it.copy(status = PlaybackStatus.PAUSED) }
                val activeBinding = binding ?: return
                val current = sessionSnapshot(activeBinding)
                telemetry.log("playback_paused", mapOf("videoCurrentTime" to current.positionTicks))
                reporter?.let { activeReporter ->
                    fireAndForget { activeReporter.progress(current, force = true) }
                }
            }
        }

        override fun onTimeUpdate(
            positionSeconds: Double,
            durationSeconds: Double,
            bufferedSeconds: Double,
            playbackAdvanced: Boolean,
        ) {
            if (closed) return
            update {
                it.copy(
                    positionSeconds = positionSeconds,
                    durationSeconds = durationSeconds,
                    bufferedSeconds = bufferedSeconds,
                )
            }
            if (playbackAdvanced) startPlaybackOnce()
            val activeBinding = binding ?: return
            reporter?.let { activeReporter ->
                val current = sessionSnapshot(activeBinding)
                fireAndForget { activeReporter.progress(current) }
            }
        }

        override fun onEnded() {
            fireAndForget { stop(PlaybackStatus.ENDED) }
        }

        override fun onError(error: PlayerErrorDetails) {
            fireAndForget { recover(error) }
        }

        private fun statusAfterReady(): PlaybackStatus = if (started) state.value.status else PlaybackStatus.READY
    }

    private companion object {
        const val PLAYER_STAGE_ID = "player"
        const val PLAYER_STAGE_LABEL = "Starting player"
        const val PLAYER_STAGE_ORDER = 1_000
    }
}
