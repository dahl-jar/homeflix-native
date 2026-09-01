package app.homeflix.tv.feature.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

private const val UNKNOWN_COLOR_TRANSFER = -1

@androidx.annotation.OptIn(UnstableApi::class)
fun playbackFormatTelemetry(
    trackType: String,
    format: Format,
    colorTransferOverride: Int? = null,
): Map<String, Any?> {
    val sampleMimeType = format.sampleMimeType
    val dolbyVision = isDolbyVision(sampleMimeType, format.codecs)
    val colorTransfer = colorTransferOverride ?: format.colorInfo?.colorTransfer ?: UNKNOWN_COLOR_TRANSFER
    return buildMap {
        put("trackType", trackType)
        sampleMimeType?.let { put("sampleMimeType", it) }
        format.codecs?.let { put("codecs", it) }
        putPositiveFormatFields(format)
        put("colorTransfer", colorTransfer)
        put("dolbyVision", dolbyVision)
        put("hdr", isHdr(dolbyVision, colorTransfer))
        put("atmos", sampleMimeType == MimeTypes.AUDIO_E_AC3_JOC)
    }
}

private fun isDolbyVision(
    sampleMimeType: String?,
    codecs: String?,
): Boolean =
    sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION ||
        codecs.orEmpty().split(',').any { codec ->
            val normalized = codec.trim().lowercase()
            normalized.startsWith("dvh1") || normalized.startsWith("dvhe")
        }

@androidx.annotation.OptIn(UnstableApi::class)
private fun isHdr(
    dolbyVision: Boolean,
    colorTransfer: Int,
): Boolean = dolbyVision || colorTransfer == C.COLOR_TRANSFER_ST2084 || colorTransfer == C.COLOR_TRANSFER_HLG

@androidx.annotation.OptIn(UnstableApi::class)
private fun MutableMap<String, Any?>.putPositiveFormatFields(format: Format) {
    if (format.width > 0) put("width", format.width)
    if (format.height > 0) put("height", format.height)
    if (format.bitrate > 0) put("bitrate", format.bitrate)
    if (format.frameRate > 0) put("frameRate", format.frameRate)
    if (format.channelCount > 0) put("channelCount", format.channelCount)
    if (format.sampleRate > 0) put("sampleRate", format.sampleRate)
}
