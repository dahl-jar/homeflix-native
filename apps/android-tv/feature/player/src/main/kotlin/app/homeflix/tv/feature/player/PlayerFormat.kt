package app.homeflix.tv.feature.player

const val PLAYER_SEEK_STEP_SECONDS = 10.0
private const val SECONDS_PER_HOUR = 3_600
private const val SECONDS_PER_MINUTE = 60
private const val MINIMUM_STAGE_WIDTH = 64f
private const val MAXIMUM_STAGE_WIDTH = 140f
private const val REASSURANCE_SLOW_MS = 8_000L
private const val REASSURANCE_LONG_MS = 20_000L
private const val SLOW_MESSAGE = "Still working on it"
private const val LONG_MESSAGE = "This can take a moment for large libraries"

enum class PlaybackStatus {
    IDLE,
    LOADING,
    READY,
    PLAYING,
    PAUSED,
    RECOVERING,
    ENDED,
    FAILED,
}

data class PipelineStageLayout(
    val centered: Boolean,
    val stageWidth: Float,
)

fun formatPlaybackTime(seconds: Double): String {
    val total = if (seconds.isFinite() && seconds > 0) seconds.toLong() else 0L
    val hours = total / SECONDS_PER_HOUR
    val minutes = (total % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val remaining = total % SECONDS_PER_MINUTE
    val minuteText = if (hours > 0) minutes.toString().padStart(2, '0') else minutes.toString()
    val secondText = remaining.toString().padStart(2, '0')
    return if (hours > 0) "$hours:$minuteText:$secondText" else "$minuteText:$secondText"
}

fun pipelineStageLayout(
    viewportWidth: Float,
    stageCount: Int,
): PipelineStageLayout {
    if (stageCount <= 0 || viewportWidth <= 0f) {
        return PipelineStageLayout(centered = true, stageWidth = MAXIMUM_STAGE_WIDTH)
    }
    val stageWidth = (viewportWidth / stageCount).coerceIn(MINIMUM_STAGE_WIDTH, MAXIMUM_STAGE_WIDTH)
    return PipelineStageLayout(
        centered = stageWidth * stageCount <= viewportWidth,
        stageWidth = stageWidth,
    )
}

fun pipelineWaitReassurance(elapsedMs: Long): String? =
    when {
        elapsedMs >= REASSURANCE_LONG_MS -> LONG_MESSAGE
        elapsedMs >= REASSURANCE_SLOW_MS -> SLOW_MESSAGE
        else -> null
    }

fun shouldScheduleAutoHide(
    status: PlaybackStatus,
    hidden: Boolean,
    pinned: Boolean,
): Boolean = status == PlaybackStatus.PLAYING && !hidden && !pinned
