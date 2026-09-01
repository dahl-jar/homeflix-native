package app.homeflix.tv.feature.player

import kotlinx.serialization.json.JsonObject

private const val TICKS_PER_SECOND = 10_000_000L

class NoCompatibleSourceException : Exception("no compatible playback source")

data class NegotiationRequest(
    val item: PlayableItem,
    val userId: String,
    val startTimeTicks: Long,
    val excludedSourceIds: Set<String>,
    val preferredMediaSourceId: String?,
    val trackOverride: TrackOverride?,
    val pipeline: PlaybackPipelineIds,
)

data class PlaybackResolution(
    val mediaSource: MediaSourceDto,
    val pipelineHandle: String,
    val pipelineDecision: String,
    val audioStreamIndex: Int,
    val subtitleStreamIndex: Int,
    val sourceCount: Int,
    val videoDelivery: String?,
    val audioDelivery: String?,
    val sourceWidth: Int?,
    val sourceHeight: Int?,
)

data class AcceptedPlayback(
    val released: ReleasedPlayback,
    val mediaSource: MediaSourceDto,
    val videoSource: PlayerMediaSource,
    val sourceCount: Int,
    val startSeconds: Double,
    val videoDelivery: String?,
    val audioDelivery: String?,
    val sourceWidth: Int?,
    val sourceHeight: Int?,
)

class PlaybackNegotiator(
    private val gateway: PlaybackInfoGateway,
    private val deviceProfile: JsonObject,
    private val baseUrl: String,
    private val watcherFactory: ProgressWatcherFactory,
    private val createPipelineId: () -> String = ::defaultPipelineId,
) {
    fun createPipeline(item: PlayableItem): PlaybackPipelineIds =
        PlaybackPipelineIds(
            itemId = item.id,
            itemName = item.name,
            pipelineId = createPipelineId(),
            now = System::currentTimeMillis,
        )

    suspend fun negotiate(
        request: NegotiationRequest,
        onEvent: (PipelineEvent) -> Unit,
    ): AcceptedPlayback {
        val attempt = request.pipeline.startAttempt()
        onEvent(PipelineEvent.ResolutionStarted)
        val resolution = resolveWithProgress(request, attempt, onEvent)
        val selected = request.pipeline.selectAttemptSource(resolution.mediaSource.id)
        onEvent(PipelineEvent.ResolutionCompleted(sourceCount = resolution.sourceCount))

        onEvent(streamStage(StageStatus.ACTIVE))
        val released = release(request, selected, resolution)
        onEvent(streamStage(StageStatus.COMPLETE))
        onEvent(PipelineEvent.ReleaseCompleted)

        return AcceptedPlayback(
            released = released,
            mediaSource = resolution.mediaSource,
            videoSource = videoSource(baseUrl, released),
            sourceCount = resolution.sourceCount,
            startSeconds = request.startTimeTicks.toDouble() / TICKS_PER_SECOND,
            videoDelivery = resolution.videoDelivery,
            audioDelivery = resolution.audioDelivery,
            sourceWidth = resolution.sourceWidth,
            sourceHeight = resolution.sourceHeight,
        )
    }

    private suspend fun resolveWithProgress(
        request: NegotiationRequest,
        attempt: PlaybackAttempt,
        onEvent: (PipelineEvent) -> Unit,
    ): PlaybackResolution {
        val watcher =
            watcherFactory.watch(
                pipelineId = request.pipeline.pipelineId,
                attemptId = attempt.attemptId,
                onEvent = onEvent,
            )
        val playbackInfo =
            try {
                gateway.resolveAttempt(
                    ResolveRequest(
                        itemId = request.item.id,
                        userId = request.userId,
                        startTimeTicks = request.startTimeTicks,
                        deviceProfile = deviceProfile,
                        pipelineId = request.pipeline.pipelineId,
                        attemptId = attempt.attemptId,
                        rejectedSourceIds = request.excludedSourceIds,
                        preferredMediaSourceId = request.trackOverride?.mediaSourceId ?: request.preferredMediaSourceId,
                        trackOverride = request.trackOverride,
                        policy = androidTvPlaybackPolicy(),
                    ),
                )
            } finally {
                watcher.stop()
            }
        if (playbackInfo.errorCode != null || playbackInfo.mediaSources.isEmpty()) {
            throw NoCompatibleSourceException()
        }
        return resolvedAttempt(playbackInfo)
    }

    private fun resolvedAttempt(playbackInfo: PlaybackInfoResult): PlaybackResolution {
        val mediaSource = playbackInfo.mediaSources.singleOrNull()
        val pipelineHandle = playbackInfo.pipelineHandle?.takeIf(String::isNotEmpty)
        val pipelineDecision = playbackInfo.pipelineDecision?.takeIf(String::isNotEmpty)
        val audioStreamIndex = playbackInfo.pipelineAudioStreamIndex
        val subtitleStreamIndex = playbackInfo.pipelineSubtitleStreamIndex
        checkNotNull(mediaSource) { "incomplete playback resolution" }
        checkNotNull(pipelineHandle) { "incomplete playback resolution" }
        checkNotNull(pipelineDecision) { "incomplete playback resolution" }
        checkNotNull(audioStreamIndex) { "incomplete playback resolution" }
        checkNotNull(subtitleStreamIndex) { "incomplete playback resolution" }
        return PlaybackResolution(
            mediaSource = mediaSource,
            pipelineHandle = pipelineHandle,
            pipelineDecision = pipelineDecision,
            audioStreamIndex = audioStreamIndex,
            subtitleStreamIndex = subtitleStreamIndex,
            sourceCount = playbackInfo.pipelineSourceCount ?: 1,
            videoDelivery = playbackInfo.videoDelivery,
            audioDelivery = playbackInfo.audioDelivery,
            sourceWidth = playbackInfo.sourceWidth,
            sourceHeight = playbackInfo.sourceHeight,
        )
    }

    private suspend fun release(
        request: NegotiationRequest,
        attempt: PlaybackAttempt,
        resolution: PlaybackResolution,
    ): ReleasedPlayback {
        val playbackInfo =
            gateway.releaseSource(
                ReleaseRequest(
                    itemId = request.item.id,
                    userId = request.userId,
                    startTimeTicks = request.startTimeTicks,
                    deviceProfile = deviceProfile,
                    pipelineId = request.pipeline.pipelineId,
                    attemptId = attempt.attemptId,
                    mediaSourceId = resolution.mediaSource.id,
                    pipelineHandle = resolution.pipelineHandle,
                    pipelineDecision = resolution.pipelineDecision,
                    audioStreamIndex = resolution.audioStreamIndex,
                    subtitleStreamIndex = resolution.subtitleStreamIndex,
                    policy = androidTvPlaybackPolicy(resolution.mediaSource),
                ),
            )
        val mediaSource =
            playbackInfo.mediaSources.find { it.id == resolution.mediaSource.id }
                ?: resolution.mediaSource
        val playSessionId =
            checkNotNull(playbackInfo.playSessionId) { "release did not return one playable source" }
        val playMethod =
            checkNotNull(playbackMethod(mediaSource) ?: playbackMethod(resolution.mediaSource)) {
                "released source has no playback method"
            }
        return ReleasedPlayback(
            itemId = request.item.id,
            mediaSourceId = mediaSource.id,
            playSessionId = playSessionId,
            playMethod = playMethod,
            audioStreamIndex = resolution.audioStreamIndex,
            subtitleStreamIndex = resolution.subtitleStreamIndex,
            pipelineId = request.pipeline.pipelineId,
            attemptId = attempt.attemptId,
            transcodingUrl = mediaSource.transcodingUrl,
            transcodingSubProtocol = mediaSource.transcodingSubProtocol,
        )
    }

    private fun streamStage(status: StageStatus): PipelineEvent.StageProgress =
        PipelineEvent.StageProgress(
            stageId = STREAM_STAGE_ID,
            label = STREAM_STAGE_LABEL,
            order = STREAM_STAGE_ORDER,
            status = status,
        )

    private companion object {
        const val STREAM_STAGE_ID = "stream"
        const val STREAM_STAGE_LABEL = "Preparing stream"
        const val STREAM_STAGE_ORDER = 900
    }
}
