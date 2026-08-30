package app.homeflix.tv.feature.player

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.view.Display
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioCapabilities

private const val DEFAULT_MAX_AUDIO_CHANNELS = 8

private val MIME_CODEC_NAMES =
    mapOf(
        MediaFormat.MIMETYPE_VIDEO_AVC to "h264",
        MediaFormat.MIMETYPE_VIDEO_HEVC to "hevc",
        MediaFormat.MIMETYPE_VIDEO_VP9 to "vp9",
        MediaFormat.MIMETYPE_VIDEO_AV1 to "av1",
        MediaFormat.MIMETYPE_VIDEO_MPEG2 to "mpeg2video",
        MediaFormat.MIMETYPE_VIDEO_MPEG4 to "mpeg4",
    )

private val DOLBY_VISION_PROFILE_NUMBERS =
    mapOf(
        MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheStn to 5,
        MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheDtb to 7,
        MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheSt to 8,
        MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvav110 to 10,
    )

@androidx.annotation.OptIn(UnstableApi::class)
fun probeTvMediaCapabilities(context: Context): TvMediaCapabilities {
    val decoders =
        MediaCodecList(MediaCodecList.REGULAR_CODECS)
            .codecInfos
            .filterNot(MediaCodecInfo::isEncoder)
    val videoCodecs =
        decoders
            .flatMap { it.supportedTypes.toList() }
            .mapNotNull { type -> MIME_CODEC_NAMES[type.lowercase()] }
            .toSet()
    val dolbyVisionProfiles =
        decoders
            .filter { decoder ->
                decoder.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION, ignoreCase = true) }
            }.flatMap { decoder ->
                decoder
                    .getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION)
                    .profileLevels
                    .mapNotNull { DOLBY_VISION_PROFILE_NUMBERS[it.profile] }
            }.toSet()
    val hdrTypes = displayHdrTypes(context)
    val audioCapabilities = AudioCapabilities.getCapabilities(context)
    val passthrough =
        buildSet {
            if (audioCapabilities.supportsEncoding(C.ENCODING_DOLBY_TRUEHD)) add("truehd")
            if (audioCapabilities.supportsEncoding(C.ENCODING_DTS)) add("dts")
        }
    return TvMediaCapabilities(
        videoCodecs = videoCodecs,
        dolbyVisionProfiles = dolbyVisionProfiles,
        displayDolbyVision = Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION in hdrTypes,
        displayHdr10 = Display.HdrCapabilities.HDR_TYPE_HDR10 in hdrTypes,
        displayHlg = Display.HdrCapabilities.HDR_TYPE_HLG in hdrTypes,
        displayHdr10Plus = Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS in hdrTypes,
        audioPassthroughCodecs = passthrough,
        maxAudioChannels = DEFAULT_MAX_AUDIO_CHANNELS,
    )
}

private fun displayHdrTypes(context: Context): Set<Int> {
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY) ?: return emptySet()
    return display.hdrCapabilities
        ?.supportedHdrTypes
        ?.toSet()
        .orEmpty()
}
