package app.homeflix.tv.feature.player

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private const val PROFILE_NAME = "Homeflix Android TV"
private const val MAX_STREAMING_BITRATE = 120_000_000
private const val DIRECT_PLAY_CONTAINERS = "mkv,mp4,m4v,mov,webm,ts,avi"
private val BASE_AUDIO_CODECS = listOf("aac", "mp3", "ac3", "eac3", "opus", "vorbis", "flac")
private val PASSTHROUGH_AUDIO_CODECS = listOf("truehd", "dts")
private const val TRANSCODE_VIDEO_CODECS = "h264,hevc"
private const val TRANSCODE_AUDIO_CODECS = "aac,ac3,eac3"
private val HDR_CAPABLE_CODECS = setOf("hevc", "vp9", "av1")
private const val DOLBY_VISION_PROFILE_FIVE = 5
private const val DOLBY_VISION_PROFILE_SEVEN = 7
private const val DOLBY_VISION_PROFILE_EIGHT = 8

fun tvDeviceProfile(capabilities: TvMediaCapabilities): JsonObject =
    buildJsonObject {
        put("Name", PROFILE_NAME)
        put("MaxStreamingBitrate", MAX_STREAMING_BITRATE)
        put("MaxStaticBitrate", MAX_STREAMING_BITRATE)
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
            capabilities.videoCodecs.sorted().forEach { codec ->
                addJsonObject {
                    put("Type", "Video")
                    put("Codec", codec)
                    putJsonArray("Conditions") {
                        addJsonObject {
                            put("Condition", "EqualsAny")
                            put("Property", "VideoRangeType")
                            put("Value", videoRangeTypes(codec, capabilities).joinToString("|"))
                            put("IsRequired", false)
                        }
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
        add("DOVIWithSDR")
        if (capabilities.displayHdr10) {
            add("HDR10")
            add("HDR10Plus")
            if (!capabilities.displayDolbyVision) {
                addAll(listOf("DOVIWithHDR10", "DOVIWithHDR10Plus", "DOVIWithEL", "DOVIWithELHDR10Plus"))
            }
        }
        if (capabilities.displayHlg) {
            add("HLG")
            if (!capabilities.displayDolbyVision) add("DOVIWithHLG")
        }
        if (capabilities.displayDolbyVision) {
            if (DOLBY_VISION_PROFILE_FIVE in capabilities.dolbyVisionProfiles) add("DOVI")
            if (DOLBY_VISION_PROFILE_EIGHT in capabilities.dolbyVisionProfiles) {
                addAll(listOf("DOVIWithHDR10", "DOVIWithHLG", "DOVIWithHDR10Plus"))
            }
            if (DOLBY_VISION_PROFILE_SEVEN in capabilities.dolbyVisionProfiles) {
                addAll(listOf("DOVIWithEL", "DOVIWithELHDR10Plus"))
            }
        }
    }.distinct()
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
