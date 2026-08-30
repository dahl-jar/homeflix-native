package app.homeflix.tv.feature.player

private const val DEFAULT_SOURCE_FAILURE_REASON = "source failed"
private const val DEFAULT_PLAYBACK_FAILURE_REASON = "playback failed"

enum class StageStatus {
    PENDING,
    ACTIVE,
    COMPLETE,
    FAILED,
}

data class PipelineStage(
    val id: String,
    val label: String,
    val order: Int,
    val status: StageStatus,
)

data class PipelineProgress(
    val attempt: Int = 1,
    val sourceOffset: Int = 0,
    val sourceAttempt: Int? = null,
    val visible: Boolean = true,
    val videoVisible: Boolean = false,
    val reason: String? = null,
    val sourceCount: Int? = null,
    val stages: List<PipelineStage> = emptyList(),
)

sealed interface PipelineEvent {
    data class StageProgress(
        val stageId: String,
        val label: String,
        val order: Int,
        val status: StageStatus,
        val sourceAttempt: Int? = null,
        val sourceCount: Int? = null,
        val reason: String? = null,
    ) : PipelineEvent

    data object ResolutionStarted : PipelineEvent

    data class ResolutionCompleted(
        val sourceCount: Int? = null,
    ) : PipelineEvent

    data object ReleaseCompleted : PipelineEvent

    data object Playing : PipelineEvent

    data object Retry : PipelineEvent

    data class Failed(
        val reason: String?,
    ) : PipelineEvent
}

fun PipelineProgress.transition(event: PipelineEvent): PipelineProgress =
    when (event) {
        is PipelineEvent.StageProgress -> applyStageProgress(event)
        PipelineEvent.ResolutionStarted -> PipelineProgress(attempt = attempt, sourceOffset = sourceOffset)
        is PipelineEvent.ResolutionCompleted ->
            copy(
                sourceCount = event.sourceCount ?: sourceCount,
                stages = stages.map { it.copy(status = StageStatus.COMPLETE) },
            )
        PipelineEvent.ReleaseCompleted -> copy(videoVisible = true)
        PipelineEvent.Playing ->
            copy(
                visible = false,
                videoVisible = true,
                stages = stages.map { it.copy(status = StageStatus.COMPLETE) },
            )
        PipelineEvent.Retry ->
            PipelineProgress(
                attempt = attempt + 1,
                sourceOffset = sourceAttempt ?: sourceOffset,
            )
        is PipelineEvent.Failed -> failPlayback(event)
    }

private fun PipelineProgress.applyStageProgress(event: PipelineEvent.StageProgress): PipelineProgress {
    if (event.stageId.isEmpty() || event.label.isEmpty()) return this
    val nextStage =
        PipelineStage(
            id = event.stageId,
            label = event.label,
            order = event.order,
            status = event.status,
        )
    val replaced =
        if (stages.any { it.id == event.stageId }) {
            stages.map { if (it.id == event.stageId) nextStage else it }
        } else {
            stages + nextStage
        }
    return copy(
        sourceAttempt = event.sourceAttempt?.let { sourceOffset + it } ?: sourceAttempt,
        sourceCount = event.sourceCount ?: sourceCount,
        reason =
            if (event.status == StageStatus.FAILED) {
                event.reason ?: DEFAULT_SOURCE_FAILURE_REASON
            } else {
                null
            },
        stages = replaced.sortedWith(compareBy(PipelineStage::order, PipelineStage::id)),
    )
}

private fun PipelineProgress.failPlayback(event: PipelineEvent.Failed): PipelineProgress {
    val activeIndex = stages.indexOfFirst { it.status == StageStatus.ACTIVE }
    val failedStages =
        if (activeIndex < 0) {
            stages
        } else {
            stages.mapIndexed { index, stage ->
                if (index == activeIndex) stage.copy(status = StageStatus.FAILED) else stage
            }
        }
    return copy(
        visible = true,
        videoVisible = false,
        reason = event.reason ?: DEFAULT_PLAYBACK_FAILURE_REASON,
        stages = failedStages,
    )
}
