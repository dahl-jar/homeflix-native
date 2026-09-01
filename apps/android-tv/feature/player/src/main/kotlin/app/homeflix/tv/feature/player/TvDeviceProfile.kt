package app.homeflix.tv.feature.player

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private const val PROFILE_NAME = "Homeflix Android TV"
private const val DEFAULT_MAX_STREAMING_BITRATE = 10_000_000
private const val DIRECT_PLAY_CONTAINERS = "mkv,mp4,m4v,mov,webm,ts,avi"
private val BASE_AUDIO_CODECS = listOf("aac", "mp3", "ac3", "eac3", "opus", "vorbis", "flac")
private val PASSTHROUGH_AUDIO_CODECS = listOf("truehd", "dts")
private const val TRANSCODE_VIDEO_CODECS = "h264,hevc"
private const val TRANSCODE_AUDIO_CODECS = "aac,ac3,eac3"
private val HDR_CAPABLE_CODECS = setOf("hevc", "vp9", "av1")

fun tvDeviceProfile(capabilities: TvMediaCapabilities): JsonObject =
    buildJsonObject {
        put("Name", PROFILE_NAME)
        val maxStreamingBitrate =
            capabilities.videoDecoders.maxOfOrNull(VideoDecoderCapability::maxBitrate)
                ?: DEFAULT_MAX_STREAMING_BITRATE
        put("MaxStreamingBitrate", maxStreamingBitrate)
        put("MaxStaticBitrate", maxStreamingBitrate)
        putJsonArray("DirectPlayProfiles") {
            addJsonObject {
                put("Type", "Video")
                put("Container", DIRECT_PLAY_CONTAINERS)
                put("VideoCodec", capabilities.videoCodecs.sorted().joinToString(","))
                put("AudioCodec", directAudioCodecs(capabilities).joinToString(","))
            }
        }
        putJsonArray("TranscodingProfiles") {
            addJsonObject {
                put("Type", "Video")
                put("Context", "Streaming")
                put("Protocol", "hls")
                put("Container", "mp4")
                put("VideoCodec", TRANSCODE_VIDEO_CODECS)
                put("AudioCodec", TRANSCODE_AUDIO_CODECS)
                put("MinSegments", "1")
                put("BreakOnNonKeyFrames", true)
            }
        }
        putJsonArray("CodecProfiles") {
            capabilities.videoDecoders.sortedBy(VideoDecoderCapability::codec).forEach { decoder ->
                addJsonObject {
                    put("Type", "Video")
                    put("Codec", decoder.codec)
                    putJsonArray("Conditions") {
                        addJsonObject {
                            put("Condition", "EqualsAny")
                            put("Property", "VideoRangeType")
                            put("Value", videoRangeTypes(decoder.codec, capabilities).joinToString("|"))
                            put("IsRequired", false)
                        }
                        limitCondition("Width", decoder.maxWidth)
                        limitCondition("Height", decoder.maxHeight)
                        limitCondition("VideoFramerate", decoder.maxFrameRate)
                        limitCondition("VideoBitrate", decoder.maxBitrate)
                    }
                }
            }
            directAudioCodecs(capabilities).forEach { codec ->
                addJsonObject {
                    put("Type", "Audio")
                    put("Codec", codec)
                    putJsonArray("Conditions") {
                        limitCondition("AudioChannels", capabilities.maxAudioChannels)
                    }
                }
            }
        }
        put("SubtitleProfiles", subtitleProfiles())
    }

private fun directAudioCodecs(capabilities: TvMediaCapabilities): List<String> =
    BASE_AUDIO_CODECS + PASSTHROUGH_AUDIO_CODECS.filter { it in capabilities.audioPassthroughCodecs }

private fun videoRangeTypes(
    codec: String,
    capabilities: TvMediaCapabilities,
): List<String> {
    if (codec !in HDR_CAPABLE_CODECS) return listOf("SDR")
    return buildList {
        add("SDR")
        if (capabilities.displayHdr10) add("HDR10")
        if (capabilities.displayHdr10Plus) add("HDR10Plus")
        if (capabilities.displayHlg) add("HLG")
        if (capabilities.displayDolbyVision) {
            add("DOVIWithSDR")
            if (DOLBY_VISION_PROFILE_5 in capabilities.dolbyVisionProfiles) add("DOVI")
            if (DOLBY_VISION_PROFILE_8 in capabilities.dolbyVisionProfiles) {
                if (capabilities.displayHdr10) add("DOVIWithHDR10")
                if (capabilities.displayHlg) add("DOVIWithHLG")
                if (capabilities.displayHdr10Plus) add("DOVIWithHDR10Plus")
            }
            if (DOLBY_VISION_PROFILE_7 in capabilities.dolbyVisionProfiles) {
                add("DOVIWithEL")
                if (capabilities.displayHdr10Plus) add("DOVIWithELHDR10Plus")
            }
        }
    }.distinct()
}

private fun kotlinx.serialization.json.JsonArrayBuilder.limitCondition(
    property: String,
    value: Int,
) {
    addJsonObject {
        put("Condition", "LessThanEqual")
        put("Property", property)
        put("Value", value.toString())
        put("IsRequired", true)
    }
}

private fun subtitleProfiles() =
    buildJsonArray {
        subtitleProfile("subrip", "External")
        subtitleProfile("ass", "Encode")
        subtitleProfile("ssa", "Encode")
        subtitleProfile("pgssub", "Embed")
        subtitleProfile("vtt", "Hls")
    }

private fun kotlinx.serialization.json.JsonArrayBuilder.subtitleProfile(
    format: String,
    method: String,
) {
    addJsonObject {
        put("Format", format)
        put("Method", method)
    }
}
