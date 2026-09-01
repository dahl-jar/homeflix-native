package app.homeflix.tv.feature.player

private const val DEFAULT_BACKWARD_TOLERANCE_SECONDS = 5.0

data class PositionDecision(
    val accepted: Boolean,
    val positionSeconds: Double,
)

class PlaybackPositionTracker(
    startPositionSeconds: Double,
    private val backwardToleranceSeconds: Double = DEFAULT_BACKWARD_TOLERANCE_SECONDS,
) {
    var confirmedPositionSeconds: Double = startPositionSeconds.coerceAtLeast(0.0)
        private set

    private var userSeekArmed = false

    fun armUserSeek(positionSeconds: Double) {
        confirmedPositionSeconds = positionSeconds.coerceAtLeast(0.0)
        userSeekArmed = true
    }

    fun update(observedPositionSeconds: Double): PositionDecision {
        val observed = observedPositionSeconds.coerceAtLeast(0.0)
        val unexplainedBackwardJump =
            !userSeekArmed && observed < confirmedPositionSeconds - backwardToleranceSeconds
        if (unexplainedBackwardJump) {
            return PositionDecision(accepted = false, positionSeconds = confirmedPositionSeconds)
        }
        userSeekArmed = false
        confirmedPositionSeconds = observed
        return PositionDecision(accepted = true, positionSeconds = observed)
    }
}
