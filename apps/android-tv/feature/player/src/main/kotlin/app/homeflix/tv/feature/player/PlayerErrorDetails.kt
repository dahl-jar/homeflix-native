package app.homeflix.tv.feature.player

import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.mediacodec.MediaCodecDecoderException

private const val ERROR_MESSAGE_LIMIT = 512

data class PlayerErrorDetails(
    val reason: String,
    val telemetry: Map<String, Any?> = emptyMap(),
)

internal data class PlayerErrorSnapshot(
    val reason: String,
    val errorType: String,
    val errorCode: Int,
    val rendererName: String? = null,
    val rendererIndex: Int? = null,
    val formatSupport: Int? = null,
    val mimeType: String? = null,
    val codecs: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val frameRate: Float? = null,
    val color: String? = null,
    val decoderName: String? = null,
    val decoderDiagnostic: String? = null,
    val decoderErrorCode: Int? = null,
)

@androidx.annotation.OptIn(UnstableApi::class)
fun playerErrorDetails(error: PlaybackException): PlayerErrorDetails {
    val exoError = error as? ExoPlaybackException
    val decoderError = exoError?.rendererFailure() as? MediaCodecDecoderException
    val format = exoError?.rendererFormat
    return playerErrorDetails(
        PlayerErrorSnapshot(
            reason = error.errorCodeName,
            errorType = decoderError?.javaClass?.simpleName ?: error.javaClass.simpleName,
            errorCode = error.errorCode,
            rendererName = exoError?.rendererName,
            rendererIndex = exoError?.rendererIndex,
            formatSupport = exoError?.rendererFormatSupport,
            mimeType = format?.sampleMimeType,
            codecs = format?.codecs,
            width = format?.width?.takeIf { it != Format.NO_VALUE },
            height = format?.height?.takeIf { it != Format.NO_VALUE },
            frameRate = format?.frameRate?.takeIf { it != Format.NO_VALUE.toFloat() },
            color = format?.colorInfo?.toLogString(),
            decoderName = decoderError?.codecInfo?.name,
            decoderDiagnostic = decoderError?.diagnosticInfo,
            decoderErrorCode = decoderError?.errorCode?.takeIf { it != 0 },
        ),
    )
}

internal fun playerErrorDetails(snapshot: PlayerErrorSnapshot): PlayerErrorDetails {
    val message = diagnosticMessage(snapshot)
    return PlayerErrorDetails(
        reason = snapshot.reason,
        telemetry =
            buildMap {
                put("errorType", snapshot.errorType)
                put("errorCode", snapshot.errorCode)
                put("errorName", snapshot.reason)
                put("errorMessage", message)
                snapshot.width?.let { put("videoWidth", it) }
                snapshot.height?.let { put("videoHeight", it) }
            },
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun ExoPlaybackException.rendererFailure(): Exception? =
    if (type == ExoPlaybackException.TYPE_RENDERER) rendererException else null

private fun diagnosticMessage(snapshot: PlayerErrorSnapshot): String =
    buildList {
        snapshot.rendererName?.let { add("renderer=$it") }
        snapshot.rendererIndex?.let { add("rendererIndex=$it") }
        snapshot.formatSupport?.let { add("formatSupport=$it") }
        snapshot.mimeType?.let { add("mime=$it") }
        snapshot.codecs?.let { add("codecs=$it") }
        addFormatDimensions(snapshot.width, snapshot.height)
        snapshot.frameRate?.let { add("frameRate=$it") }
        snapshot.color?.let { add("color=$it") }
        snapshot.decoderName?.let { add("decoder=$it") }
        snapshot.decoderDiagnostic?.let { add("diagnostic=$it") }
        snapshot.decoderErrorCode?.let { add("codecError=$it") }
    }.joinToString(separator = "; ").take(ERROR_MESSAGE_LIMIT)

private fun MutableList<String>.addFormatDimensions(
    width: Int?,
    height: Int?,
) {
    if (width != null || height != null) add("size=${width ?: "?"}x${height ?: "?"}")
}
