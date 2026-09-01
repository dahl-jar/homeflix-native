package app.homeflix.tv.feature.player

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.view.Display
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioCapabilities

private const val AFTKRT_MODEL = "AFTKRT"
private const val AFTKRT_HEVC_MAX_BITRATE = 35_000_000
private const val AFTKRT_HEVC_MAX_FRAME_RATE = 60

private fun mimeCodecNames(): Map<String, String> =
    buildMap {
        put(MediaFormat.MIMETYPE_VIDEO_AVC, "h264")
        put(MediaFormat.MIMETYPE_VIDEO_HEVC, "hevc")
        put(MediaFormat.MIMETYPE_VIDEO_VP9, "vp9")
        put(MediaFormat.MIMETYPE_VIDEO_MPEG2, "mpeg2video")
        put(MediaFormat.MIMETYPE_VIDEO_MPEG4, "mpeg4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaFormat.MIMETYPE_VIDEO_AV1, "av1")
        }
    }

private fun dolbyVisionProfileNumbers(): Map<Int, Int> =
    buildMap {
        put(MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheStn, DOLBY_VISION_PROFILE_5)
        put(MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheDtb, DOLBY_VISION_PROFILE_7)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            put(MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheSt, DOLBY_VISION_PROFILE_8)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            put(MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvav110, DOLBY_VISION_PROFILE_10)
        }
    }

@androidx.annotation.OptIn(UnstableApi::class)
fun probeTvMediaCapabilities(context: Context): TvMediaCapabilities {
    val decoders =
        MediaCodecList(MediaCodecList.REGULAR_CODECS)
            .codecInfos
            .filterNot(MediaCodecInfo::isEncoder)
    val videoDecoders = probeVideoDecoders(decoders, mimeCodecNames())
    val profileNumbers = dolbyVisionProfileNumbers()
    val dolbyVisionProfiles =
        decoders
            .filter { decoder ->
                decoder.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION, ignoreCase = true) }
            }.flatMap { decoder ->
                decoder
                    .getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION)
                    .profileLevels
                    .mapNotNull { profileNumbers[it.profile] }
            }.toSet()
    val hdrTypes = displayHdrTypes(context)
    val audioCapabilities = AudioCapabilities.getCapabilities(context, AudioAttributes.DEFAULT, null)
    val passthrough =
        buildSet {
            if (audioCapabilities.supportsEncoding(C.ENCODING_DOLBY_TRUEHD)) add("truehd")
            if (audioCapabilities.supportsEncoding(C.ENCODING_DTS)) add("dts")
        }
    return TvMediaCapabilities(
        videoDecoders = videoDecoders,
        dolbyVisionProfiles = dolbyVisionProfiles,
        displayDolbyVision = Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION in hdrTypes,
        displayHdr10 = Display.HdrCapabilities.HDR_TYPE_HDR10 in hdrTypes,
        displayHlg = Display.HdrCapabilities.HDR_TYPE_HLG in hdrTypes,
        displayHdr10Plus =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS in hdrTypes,
        audioPassthroughCodecs = passthrough,
        maxAudioChannels = audioCapabilities.maxChannelCount.coerceAtLeast(2),
    )
}

private fun probeVideoDecoders(
    decoders: List<MediaCodecInfo>,
    mimeCodecNames: Map<String, String>,
): List<VideoDecoderCapability> =
    decoders
        .flatMap { decoder ->
            decoder.supportedTypes.mapNotNull { type ->
                mimeCodecNames[type.lowercase()]?.let { codec -> decoderCapability(decoder, type, codec) }
            }
        }.groupBy(VideoDecoderCapability::codec)
        .map { (codec, capabilities) ->
            VideoDecoderCapability(
                codec = codec,
                maxWidth = capabilities.maxOf(VideoDecoderCapability::maxWidth),
                maxHeight = capabilities.maxOf(VideoDecoderCapability::maxHeight),
                maxFrameRate = capabilities.maxOf(VideoDecoderCapability::maxFrameRate),
                maxBitrate = capabilities.maxOf(VideoDecoderCapability::maxBitrate),
            ).applyDeviceQuirks()
        }

private fun decoderCapability(
    decoder: MediaCodecInfo,
    type: String,
    codec: String,
): VideoDecoderCapability? =
    runCatching {
        val video = decoder.getCapabilitiesForType(type).videoCapabilities ?: return null
        VideoDecoderCapability(
            codec = codec,
            maxWidth = video.supportedWidths.upper,
            maxHeight = video.supportedHeights.upper,
            maxFrameRate = video.supportedFrameRates.upper.toInt(),
            maxBitrate = video.bitrateRange.upper,
        )
    }.getOrNull()

private fun VideoDecoderCapability.applyDeviceQuirks(): VideoDecoderCapability =
    if (Build.MODEL == AFTKRT_MODEL && codec == "hevc") {
        copy(
            maxFrameRate = minOf(maxFrameRate, AFTKRT_HEVC_MAX_FRAME_RATE),
            maxBitrate = minOf(maxBitrate, AFTKRT_HEVC_MAX_BITRATE),
        )
    } else {
        this
    }

private fun displayHdrTypes(context: Context): Set<Int> {
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY) ?: return emptySet()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        display.mode.supportedHdrTypes.toSet()
    } else {
        legacyDisplayHdrTypes(display)
    }
}

@Suppress("DEPRECATION")
private fun legacyDisplayHdrTypes(display: Display): Set<Int> =
    display.hdrCapabilities
        ?.supportedHdrTypes
        ?.toSet()
        .orEmpty()
