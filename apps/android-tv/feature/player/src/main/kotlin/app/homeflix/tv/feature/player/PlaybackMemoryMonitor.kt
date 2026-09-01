package app.homeflix.tv.feature.player

import android.os.Debug

const val PLAYBACK_MEMORY_SAMPLE_INTERVAL_MS = 10_000L

data class PlaybackMemoryUsage(
    val heapUsedBytes: Long,
    val heapMaxBytes: Long,
    val nativeHeapAllocatedBytes: Long,
    val totalPssKb: Long,
)

fun capturePlaybackMemoryUsage(): PlaybackMemoryUsage {
    val runtime = Runtime.getRuntime()
    return PlaybackMemoryUsage(
        heapUsedBytes = runtime.totalMemory() - runtime.freeMemory(),
        heapMaxBytes = runtime.maxMemory(),
        nativeHeapAllocatedBytes = Debug.getNativeHeapAllocatedSize(),
        totalPssKb = Debug.getPss(),
    )
}

fun playbackMemoryTelemetry(
    usage: PlaybackMemoryUsage,
    playbackStatus: PlaybackStatus,
    engineState: PlaybackEngineState,
    positionSeconds: Double,
    bufferedSeconds: Double,
): Map<String, Any?> =
    mapOf(
        "heapUsedBytes" to usage.heapUsedBytes,
        "heapMaxBytes" to usage.heapMaxBytes,
        "nativeHeapAllocatedBytes" to usage.nativeHeapAllocatedBytes,
        "totalPssKb" to usage.totalPssKb,
        "playbackStatus" to playbackStatus.name.lowercase(),
        "engineState" to engineState.name.lowercase(),
        "videoCurrentTime" to positionSeconds,
        "bufferedDurationSeconds" to (bufferedSeconds - positionSeconds).coerceAtLeast(0.0),
    )
