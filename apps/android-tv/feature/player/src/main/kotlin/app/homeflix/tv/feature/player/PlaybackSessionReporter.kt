package app.homeflix.tv.feature.player

import kotlinx.coroutines.CancellationException

private const val DEFAULT_PROGRESS_INTERVAL_MS = 10_000L

class PlaybackSessionReporter(
    private val gateway: SessionReportGateway,
    private val context: SessionContext,
    private val now: () -> Long = System::currentTimeMillis,
    private val intervalMs: Long = DEFAULT_PROGRESS_INTERVAL_MS,
) {
    private var started = false
    private var stopped = false
    private var lastProgressAt = 0L

    suspend fun start(snapshot: SessionSnapshot): Boolean {
        if (started || stopped) return false
        val sent = send { gateway.reportStart(context, snapshot) }
        if (sent) {
            started = true
            lastProgressAt = now()
        }
        return sent
    }

    suspend fun progress(
        snapshot: SessionSnapshot,
        force: Boolean = false,
    ): Boolean {
        val currentTime = now()
        val active = started && !stopped
        val due = active && (force || currentTime - lastProgressAt >= intervalMs)
        if (!due) return false
        val sent = send { gateway.reportProgress(context, snapshot) }
        if (sent) lastProgressAt = currentTime
        return sent
    }

    suspend fun stop(snapshot: SessionSnapshot): Boolean {
        if (stopped) return false
        stopped = true
        return send { gateway.reportStop(context, snapshot) }
    }

    private suspend fun send(report: suspend () -> Unit): Boolean =
        try {
            report()
            true
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: Exception) {
            false
        }
}
