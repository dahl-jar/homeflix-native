package app.homeflix.tv.feature.player

import kotlin.math.max

private const val STARTUP_TIMEOUT_MS = 30_000L
private const val REBUFFER_TIMEOUT_MS = 15_000L
private const val EARLY_END_TOLERANCE_SECONDS = 30.0
private const val EARLY_END_TOLERANCE_FRACTION = 0.02
private const val MINIMUM_PROGRESS_SECONDS = 0.1

enum class PlaybackEngineState {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
}

enum class PlaybackHealthFailure {
    STARTUP_TIMEOUT,
    REBUFFER_TIMEOUT,
    EARLY_END,
    BACKWARD_JUMP,
}

class PlaybackHealthMonitor {
    private var loadStartedAtMs: Long? = null
    private var bufferingStartedAtMs: Long? = null
    private var firstFrameRendered = false
    private var playWhenReady = false
    private var state = PlaybackEngineState.IDLE
    private var lastPositionSeconds = 0.0

    fun onLoad(nowMs: Long) {
        loadStartedAtMs = nowMs
        bufferingStartedAtMs = null
        firstFrameRendered = false
        playWhenReady = false
        state = PlaybackEngineState.IDLE
        lastPositionSeconds = 0.0
    }

    fun onFirstFrame(nowMs: Long) {
        firstFrameRendered = true
        if (state == PlaybackEngineState.BUFFERING) bufferingStartedAtMs = nowMs
    }

    fun onPosition(
        positionSeconds: Double,
        nowMs: Long,
    ) {
        if (positionSeconds > lastPositionSeconds + MINIMUM_PROGRESS_SECONDS) {
            lastPositionSeconds = positionSeconds
            if (state == PlaybackEngineState.BUFFERING) bufferingStartedAtMs = nowMs
        }
    }

    fun onState(
        state: PlaybackEngineState,
        playWhenReady: Boolean,
        nowMs: Long,
    ) {
        val startedBuffering = state == PlaybackEngineState.BUFFERING && this.state != PlaybackEngineState.BUFFERING
        this.state = state
        this.playWhenReady = playWhenReady
        bufferingStartedAtMs =
            when {
                state != PlaybackEngineState.BUFFERING -> null
                startedBuffering -> nowMs
                else -> bufferingStartedAtMs
            }
    }

    fun evaluate(nowMs: Long): PlaybackHealthFailure? =
        when {
            !playWhenReady || loadStartedAtMs == null -> null
            startupTimedOut(nowMs, checkNotNull(loadStartedAtMs)) -> PlaybackHealthFailure.STARTUP_TIMEOUT
            rebufferTimedOut(nowMs) -> PlaybackHealthFailure.REBUFFER_TIMEOUT
            else -> null
        }

    private fun startupTimedOut(
        nowMs: Long,
        loadStartedAtMs: Long,
    ): Boolean = !firstFrameRendered && nowMs - loadStartedAtMs >= STARTUP_TIMEOUT_MS

    private fun rebufferTimedOut(nowMs: Long): Boolean =
        bufferingStartedAtMs?.let { startedAtMs ->
            firstFrameRendered &&
                state == PlaybackEngineState.BUFFERING &&
                nowMs - startedAtMs >= REBUFFER_TIMEOUT_MS
        } ?: false

    fun onEnded(
        positionSeconds: Double,
        expectedDurationSeconds: Double,
    ): PlaybackHealthFailure? {
        val tolerance = max(EARLY_END_TOLERANCE_SECONDS, expectedDurationSeconds * EARLY_END_TOLERANCE_FRACTION)
        return if (expectedDurationSeconds - positionSeconds > tolerance) {
            PlaybackHealthFailure.EARLY_END
        } else {
            null
        }
    }
}
