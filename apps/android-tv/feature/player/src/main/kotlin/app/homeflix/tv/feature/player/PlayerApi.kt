package app.homeflix.tv.feature.player

import app.homeflix.tv.core.network.JsonApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private const val BACKDROP_MAX_WIDTH = 1280
private const val EPISODE_STILL_MAX_WIDTH = 400
private const val IMAGE_QUALITY = 90
private const val NEXT_UP_LIMIT = 1
private const val FOLLOWING_EPISODES_LIMIT = 2

@Suppress("TooManyFunctions")
class PlayerApi(
    private val baseUrl: String,
    private val client: JsonApiClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : PlayerGateway {
    override suspend fun resolveAttempt(request: ResolveRequest): PlaybackInfoResult {
        val body =
            buildJsonObject {
                putPlaybackBase(request.userId, request.startTimeTicks, request.deviceProfile)
                put("PlaybackPipelineId", request.pipelineId)
                put("PlaybackAttemptId", request.attemptId)
                put("PlaybackPipelineResolve", true)
                putJsonArray("PlaybackRejectedSourceIds") {
                    request.rejectedSourceIds.sorted().forEach { add(it) }
                }
                request.preferredMediaSourceId?.let { put("PlaybackPreferredMediaSourceId", it) }
                request.trackOverride?.let { override ->
                    put("PlaybackPipelineTrackOverride", true)
                    put("AudioStreamIndex", override.audioStreamIndex)
                    put("SubtitleStreamIndex", override.subtitleStreamIndex)
                }
            }
        return parsePlaybackInfo(client.post("/Items/${request.itemId}/PlaybackInfo", body.toString()))
    }

    override suspend fun releaseSource(request: ReleaseRequest): PlaybackInfoResult {
        val body =
            buildJsonObject {
                putPlaybackBase(request.userId, request.startTimeTicks, request.deviceProfile)
                put("MediaSourceId", request.mediaSourceId)
                put("PlaybackPipelineId", request.pipelineId)
                put("PlaybackAttemptId", request.attemptId)
                put("PlaybackPipelineHandle", request.pipelineHandle)
                put("PlaybackPipelineAccepted", true)
                put("PlaybackPipelineDecision", request.pipelineDecision)
                put("AudioStreamIndex", request.audioStreamIndex)
                put("SubtitleStreamIndex", request.subtitleStreamIndex)
            }
        return parsePlaybackInfo(client.post("/Items/${request.itemId}/PlaybackInfo", body.toString()))
    }

    override suspend fun pipelineProgress(
        pipelineId: String,
        attemptId: String,
        afterSequence: Long,
    ): List<PipelineEventDto> {
        val payload =
            client.get(
                path = "/Playback/PipelineProgress",
                query =
                    mapOf(
                        "pipelineId" to pipelineId,
                        "attemptId" to attemptId,
                        "afterSequence" to afterSequence.toString(),
                    ),
            )
        return json.decodeFromString<PipelineProgressResponse>(payload).events.map { event ->
            PipelineEventDto(
                sequence = event.sequence,
                stageId = event.stageId,
                label = event.label,
                order = event.order,
                status = event.status,
                sourceAttempt = event.sourceAttempt,
                sourceCount = event.sourceCount,
                reason = event.reason,
            )
        }
    }

    override suspend fun reportStart(
        context: SessionContext,
        snapshot: SessionSnapshot,
    ) {
        client.post("/Sessions/Playing", sessionPayload(context, snapshot).toString())
    }

    override suspend fun reportProgress(
        context: SessionContext,
        snapshot: SessionSnapshot,
    ) {
        client.post("/Sessions/Playing/Progress", sessionPayload(context, snapshot).toString())
    }

    override suspend fun reportStop(
        context: SessionContext,
        snapshot: SessionSnapshot,
    ) {
        val payload =
            buildJsonObject {
                sessionPayload(context, snapshot).forEach { (key, value) -> put(key, value) }
                put("Failed", snapshot.failed)
            }
        client.post("/Sessions/Playing/Stopped", payload.toString())
    }

    override suspend fun logPipelineEvent(payload: JsonObject) {
        client.post("/ClientLog/PlaybackPipeline", payload.toString())
    }

    override suspend fun fetchItem(
        userId: String,
        itemId: String,
    ): PlayableItem {
        val payload =
            client.get(
                path = "/Users/$userId/Items/$itemId",
                query =
                    mapOf(
                        "includeMediaSources" to "false",
                        "includeMediaStreams" to "false",
                        "waitForSeriesTree" to "false",
                    ),
            )
        return json.decodeFromString<PlayableItemDto>(payload).toPlayableItem(baseUrl)
    }

    override suspend fun nextUpEpisode(
        userId: String,
        seriesId: String,
    ): PlayableItem? {
        val payload =
            client.get(
                path = "/Shows/NextUp",
                query =
                    mapOf(
                        "userId" to userId,
                        "seriesId" to seriesId,
                        "limit" to NEXT_UP_LIMIT.toString(),
                        "enableResumable" to "true",
                        "enableRewatching" to "true",
                        "enableUserData" to "true",
                        "enableTotalRecordCount" to "false",
                    ),
            )
        return json
            .decodeFromString<PlayableItemsResponse>(payload)
            .items
            .firstOrNull()
            ?.toPlayableItem(baseUrl)
    }

    override suspend fun followingEpisodes(
        userId: String,
        seriesId: String,
        itemId: String,
    ): List<PlayableItem> {
        val payload =
            client.get(
                path = "/Shows/$seriesId/Episodes",
                query =
                    mapOf(
                        "userId" to userId,
                        "startItemId" to itemId,
                        "limit" to FOLLOWING_EPISODES_LIMIT.toString(),
                        "isMissing" to "false",
                        "enableImages" to "true",
                        "enableUserData" to "true",
                        "enableTotalRecordCount" to "false",
                        "fields" to "Overview,PrimaryImageAspectRatio",
                    ),
            )
        return json.decodeFromString<PlayableItemsResponse>(payload).items.map { it.toPlayableItem(baseUrl) }
    }

    override suspend fun seriesEpisodes(
        userId: String,
        seriesId: String,
        itemId: String,
    ): List<PlayableItem> {
        val payload =
            client.get(
                path = "/Shows/$seriesId/Episodes",
                query =
                    mapOf(
                        "userId" to userId,
                        "startItemId" to itemId,
                        "isMissing" to "false",
                        "enableImages" to "true",
                        "enableUserData" to "true",
                        "enableTotalRecordCount" to "false",
                        "fields" to "Overview,PrimaryImageAspectRatio",
                    ),
            )
        return json.decodeFromString<PlayableItemsResponse>(payload).items.map { it.toPlayableItem(baseUrl) }
    }

    override suspend fun mediaSegments(itemId: String): List<SegmentDto> {
        val payload =
            client.get(
                path = "/MediaSegments/$itemId",
                query = mapOf("includeSegmentTypes" to "Intro,Recap,Outro"),
            )
        return json.decodeFromString<MediaSegmentsResponse>(payload).items.map { segment ->
            SegmentDto(
                id = segment.id,
                type = segment.type,
                startTicks = segment.startTicks,
                endTicks = segment.endTicks,
            )
        }
    }

    private fun parsePlaybackInfo(payload: String): PlaybackInfoResult {
        val response = json.decodeFromString<PlaybackInfoResponse>(payload)
        return PlaybackInfoResult(
            playSessionId = response.playSessionId,
            errorCode = response.errorCode,
            mediaSources = response.mediaSources.map(MediaSourceResponse::toMediaSource),
            pipelineHandle = response.pipelineHandle,
            pipelineDecision = response.pipelineDecision,
            pipelineAudioStreamIndex = response.pipelineAudioStreamIndex,
            pipelineSubtitleStreamIndex = response.pipelineSubtitleStreamIndex,
            pipelineSourceCount = response.pipelineSourceCount,
            videoDelivery = response.videoDelivery,
            audioDelivery = response.audioDelivery,
            sourceWidth = response.sourceWidth,
            sourceHeight = response.sourceHeight,
        )
    }
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putPlaybackBase(
    userId: String,
    startTimeTicks: Long,
    deviceProfile: JsonObject,
) {
    put("UserId", userId)
    put("DeviceProfile", deviceProfile)
    put("StartTimeTicks", startTimeTicks)
    put("EnableDirectPlay", true)
    put("EnableDirectStream", true)
    put("EnableTranscoding", true)
    put("AllowVideoStreamCopy", true)
    put("AllowAudioStreamCopy", true)
}

private fun sessionPayload(
    context: SessionContext,
    snapshot: SessionSnapshot,
): JsonObject =
    buildJsonObject {
        put("ItemId", context.itemId)
        put("MediaSourceId", context.mediaSourceId)
        put("PlaySessionId", context.playSessionId)
        put("PlaybackPipelineId", context.pipelineId)
        put("PlaybackAttemptId", context.attemptId)
        put("PlayMethod", context.playMethod.wireName)
        put("AudioStreamIndex", context.audioStreamIndex)
        put("SubtitleStreamIndex", context.subtitleStreamIndex)
        put("CanSeek", true)
        put("IsPaused", snapshot.isPaused)
        put("IsMuted", false)
        put("PositionTicks", snapshot.positionTicks)
    }

@Serializable
private data class PlaybackInfoResponse(
    @SerialName("PlaySessionId") val playSessionId: String? = null,
    @SerialName("ErrorCode") val errorCode: String? = null,
    @SerialName("MediaSources") val mediaSources: List<MediaSourceResponse> = emptyList(),
    @SerialName("PlaybackPipelineHandle") val pipelineHandle: String? = null,
    @SerialName("PlaybackPipelineDecision") val pipelineDecision: String? = null,
    @SerialName("PlaybackPipelineAudioStreamIndex") val pipelineAudioStreamIndex: Int? = null,
    @SerialName("PlaybackPipelineSubtitleStreamIndex") val pipelineSubtitleStreamIndex: Int? = null,
    @SerialName("PlaybackPipelineSourceCount") val pipelineSourceCount: Int? = null,
    @SerialName("PlaybackPipelineVideoDelivery") val videoDelivery: String? = null,
    @SerialName("PlaybackPipelineAudioDelivery") val audioDelivery: String? = null,
    @SerialName("PlaybackPipelineSourceWidth") val sourceWidth: Int? = null,
    @SerialName("PlaybackPipelineSourceHeight") val sourceHeight: Int? = null,
)

@Serializable
private data class MediaSourceResponse(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String? = null,
    @SerialName("SupportsDirectPlay") val supportsDirectPlay: Boolean = false,
    @SerialName("SupportsDirectStream") val supportsDirectStream: Boolean = false,
    @SerialName("SupportsTranscoding") val supportsTranscoding: Boolean = false,
    @SerialName("TranscodingUrl") val transcodingUrl: String? = null,
    @SerialName("TranscodingSubProtocol") val transcodingSubProtocol: String? = null,
    @SerialName("MediaStreams") val mediaStreams: List<MediaStreamResponse> = emptyList(),
) {
    fun toMediaSource(): MediaSourceDto =
        MediaSourceDto(
            id = id,
            name = name,
            supportsDirectPlay = supportsDirectPlay,
            supportsDirectStream = supportsDirectStream,
            supportsTranscoding = supportsTranscoding,
            transcodingUrl = transcodingUrl,
            transcodingSubProtocol = transcodingSubProtocol,
            mediaStreams =
                mediaStreams.map { stream ->
                    MediaStreamDto(
                        index = stream.index,
                        type = stream.type,
                        language = stream.language,
                        displayTitle = stream.displayTitle,
                        title = stream.title,
                        channels = stream.channels,
                        isForced = stream.isForced,
                        isHearingImpaired = stream.isHearingImpaired,
                        isExternal = stream.isExternal,
                    )
                },
        )
}

@Serializable
private data class MediaStreamResponse(
    @SerialName("Index") val index: Int = -1,
    @SerialName("Type") val type: String = "",
    @SerialName("Language") val language: String? = null,
    @SerialName("DisplayTitle") val displayTitle: String? = null,
    @SerialName("Title") val title: String? = null,
    @SerialName("Channels") val channels: Int? = null,
    @SerialName("IsForced") val isForced: Boolean = false,
    @SerialName("IsHearingImpaired") val isHearingImpaired: Boolean = false,
    @SerialName("IsExternal") val isExternal: Boolean? = null,
)

@Serializable
private data class PipelineProgressResponse(
    @SerialName("Events") val events: List<PipelineProgressEventResponse> = emptyList(),
)

@Serializable
private data class PipelineProgressEventResponse(
    @SerialName("Sequence") val sequence: Long = 0,
    @SerialName("StageId") val stageId: String? = null,
    @SerialName("Label") val label: String? = null,
    @SerialName("Order") val order: Int? = null,
    @SerialName("Status") val status: String? = null,
    @SerialName("SourceAttempt") val sourceAttempt: Int? = null,
    @SerialName("SourceCount") val sourceCount: Int? = null,
    @SerialName("Reason") val reason: String? = null,
)

@Serializable
private data class MediaSegmentsResponse(
    @SerialName("Items") val items: List<MediaSegmentResponse> = emptyList(),
)

@Serializable
private data class MediaSegmentResponse(
    @SerialName("Id") val id: String? = null,
    @SerialName("Type") val type: String? = null,
    @SerialName("StartTicks") val startTicks: Long? = null,
    @SerialName("EndTicks") val endTicks: Long? = null,
)

@Serializable
private data class PlayableItemsResponse(
    @SerialName("Items") val items: List<PlayableItemDto> = emptyList(),
)

@Serializable
private data class PlayableItemDto(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String = "",
    @SerialName("Type") val type: String = "",
    @SerialName("SeriesId") val seriesId: String? = null,
    @SerialName("SeriesName") val seriesName: String? = null,
    @SerialName("IndexNumber") val indexNumber: Int? = null,
    @SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @SerialName("IsMissing") val isMissing: Boolean = false,
    @SerialName("Overview") val overview: String? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("ImageTags") val imageTags: Map<String, String> = emptyMap(),
    @SerialName("UserData") val userData: PlayableUserDataDto? = null,
    @SerialName("BackdropImageTags") val backdropImageTags: List<String> = emptyList(),
    @SerialName("ParentBackdropItemId") val parentBackdropItemId: String? = null,
    @SerialName("ParentBackdropImageTags") val parentBackdropImageTags: List<String> = emptyList(),
) {
    fun toPlayableItem(baseUrl: String): PlayableItem =
        PlayableItem(
            id = id,
            name = name,
            type = type,
            seriesId = seriesId,
            seriesName = seriesName,
            indexNumber = indexNumber,
            parentIndexNumber = parentIndexNumber,
            isMissing = isMissing,
            resumePositionTicks = userData?.playbackPositionTicks ?: 0,
            backdropUrl = backdropUrl(baseUrl),
            overview = overview,
            runTimeTicks = runTimeTicks,
            primaryImageUrl = primaryImageUrl(baseUrl),
        )

    private fun primaryImageUrl(baseUrl: String): String? {
        val tag = imageTags["Primary"] ?: return null
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        return "$normalizedBaseUrl/Items/$id/Images/Primary?tag=$tag" +
            "&maxWidth=$EPISODE_STILL_MAX_WIDTH&quality=$IMAGE_QUALITY"
    }

    private fun backdropUrl(baseUrl: String): String? {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val ownBackdrop = backdropImageTags.firstOrNull()
        val parentTag = parentBackdropImageTags.firstOrNull()
        val parentId = parentBackdropItemId
        return when {
            ownBackdrop != null -> backdropImageUrl(normalizedBaseUrl, id, ownBackdrop)
            parentTag != null && parentId != null -> backdropImageUrl(normalizedBaseUrl, parentId, parentTag)
            else -> null
        }
    }
}

@Serializable
private data class PlayableUserDataDto(
    @SerialName("PlaybackPositionTicks") val playbackPositionTicks: Long = 0,
)

private fun backdropImageUrl(
    baseUrl: String,
    itemId: String,
    tag: String,
): String = "$baseUrl/Items/$itemId/Images/Backdrop/0?tag=$tag&maxWidth=$BACKDROP_MAX_WIDTH&quality=$IMAGE_QUALITY"
