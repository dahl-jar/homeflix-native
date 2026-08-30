package app.homeflix.tv.feature.player

import kotlinx.serialization.json.JsonObject

data class PlayableItem(
    val id: String,
    val name: String,
    val type: String,
    val seriesId: String?,
    val seriesName: String?,
    val indexNumber: Int?,
    val parentIndexNumber: Int?,
    val isMissing: Boolean,
    val resumePositionTicks: Long,
    val backdropUrl: String?,
    val overview: String? = null,
    val runTimeTicks: Long? = null,
    val primaryImageUrl: String? = null,
)

data class MediaStreamDto(
    val index: Int,
    val type: String,
    val language: String?,
    val displayTitle: String?,
    val title: String?,
    val channels: Int?,
    val isForced: Boolean,
    val isHearingImpaired: Boolean,
    val isExternal: Boolean?,
)

data class MediaSourceDto(
    val id: String,
    val name: String?,
    val supportsDirectPlay: Boolean,
    val supportsDirectStream: Boolean,
    val supportsTranscoding: Boolean,
    val transcodingUrl: String?,
    val transcodingSubProtocol: String?,
    val mediaStreams: List<MediaStreamDto>,
)

data class PlaybackInfoResult(
    val playSessionId: String?,
    val errorCode: String?,
    val mediaSources: List<MediaSourceDto>,
    val pipelineHandle: String?,
    val pipelineDecision: String?,
    val pipelineAudioStreamIndex: Int?,
    val pipelineSubtitleStreamIndex: Int?,
    val pipelineSourceCount: Int?,
    val videoDelivery: String?,
    val audioDelivery: String?,
    val sourceWidth: Int?,
    val sourceHeight: Int?,
)

data class PipelineEventDto(
    val sequence: Long,
    val stageId: String?,
    val label: String?,
    val order: Int?,
    val status: String?,
    val sourceAttempt: Int?,
    val sourceCount: Int?,
    val reason: String?,
)

data class SegmentDto(
    val id: String?,
    val type: String?,
    val startTicks: Long?,
    val endTicks: Long?,
)

enum class PlayMethod(
    val wireName: String,
) {
    DIRECT_PLAY("DirectPlay"),
    DIRECT_STREAM("DirectStream"),
    TRANSCODE("Transcode"),
}

data class TrackOverride(
    val mediaSourceId: String,
    val audioStreamIndex: Int,
    val subtitleStreamIndex: Int,
)

data class ResolveRequest(
    val itemId: String,
    val userId: String,
    val startTimeTicks: Long,
    val deviceProfile: JsonObject,
    val pipelineId: String,
    val attemptId: String,
    val rejectedSourceIds: Set<String>,
    val preferredMediaSourceId: String?,
    val trackOverride: TrackOverride?,
)

data class ReleaseRequest(
    val itemId: String,
    val userId: String,
    val startTimeTicks: Long,
    val deviceProfile: JsonObject,
    val pipelineId: String,
    val attemptId: String,
    val mediaSourceId: String,
    val pipelineHandle: String,
    val pipelineDecision: String,
    val audioStreamIndex: Int,
    val subtitleStreamIndex: Int,
)

data class SessionContext(
    val itemId: String,
    val mediaSourceId: String,
    val playSessionId: String,
    val pipelineId: String,
    val attemptId: String,
    val playMethod: PlayMethod,
    val audioStreamIndex: Int,
    val subtitleStreamIndex: Int,
)

data class SessionSnapshot(
    val positionTicks: Long,
    val isPaused: Boolean,
    val failed: Boolean = false,
)

interface PlaybackInfoGateway {
    suspend fun resolveAttempt(request: ResolveRequest): PlaybackInfoResult

    suspend fun releaseSource(request: ReleaseRequest): PlaybackInfoResult
}

interface PipelineProgressGateway {
    suspend fun pipelineProgress(
        pipelineId: String,
        attemptId: String,
        afterSequence: Long,
    ): List<PipelineEventDto>
}

interface SessionReportGateway {
    suspend fun reportStart(
        context: SessionContext,
        snapshot: SessionSnapshot,
    )

    suspend fun reportProgress(
        context: SessionContext,
        snapshot: SessionSnapshot,
    )

    suspend fun reportStop(
        context: SessionContext,
        snapshot: SessionSnapshot,
    )
}

interface TelemetryGateway {
    suspend fun logPipelineEvent(payload: JsonObject)
}

interface PlayerItemGateway {
    suspend fun fetchItem(
        userId: String,
        itemId: String,
    ): PlayableItem

    suspend fun nextUpEpisode(
        userId: String,
        seriesId: String,
    ): PlayableItem?

    suspend fun followingEpisodes(
        userId: String,
        seriesId: String,
        itemId: String,
    ): List<PlayableItem>

    suspend fun seriesEpisodes(
        userId: String,
        seriesId: String,
        itemId: String,
    ): List<PlayableItem>

    suspend fun mediaSegments(itemId: String): List<SegmentDto>
}

interface PlayerGateway :
    PlaybackInfoGateway,
    PipelineProgressGateway,
    SessionReportGateway,
    TelemetryGateway,
    PlayerItemGateway
