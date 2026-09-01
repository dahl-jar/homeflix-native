package app.homeflix.tv.feature.player

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TvDeviceProfileTest {
    @Test
    fun `should include truehd and dts only with passthrough`() {
        val silent = tvDeviceProfile(sdrCapabilities())
        val passthrough =
            tvDeviceProfile(sdrCapabilities().copy(audioPassthroughCodecs = setOf("truehd", "dts")))

        val silentAudio = directAudioCodecs(silent)
        val passthroughAudio = directAudioCodecs(passthrough)
        assertFalse(silentAudio.contains("truehd"))
        assertFalse(silentAudio.contains("dts"))
        assertTrue(passthroughAudio.contains("truehd"))
        assertTrue(passthroughAudio.contains("dts"))
    }

    @Test
    fun `should keep only sdr without hdr display`() {
        val ranges = hevcRangeTypes(tvDeviceProfile(sdrCapabilities()))

        assertEquals(setOf("SDR"), ranges)
    }

    @Test
    fun `should not advertise hdr10 plus or dovi for hdr10 display`() {
        val ranges =
            hevcRangeTypes(tvDeviceProfile(sdrCapabilities().copy(displayHdr10 = true, displayHlg = true)))

        assertEquals(setOf("SDR", "HDR10", "HLG"), ranges)
        assertFalse(ranges.contains("HDR10Plus"))
        assertFalse(ranges.contains("DOVI"))
    }

    @Test
    fun `should advertise hdr10 plus only when display supports it`() {
        val ranges =
            hevcRangeTypes(
                tvDeviceProfile(
                    sdrCapabilities().copy(displayHdr10 = true, displayHdr10Plus = true),
                ),
            )

        assertTrue(ranges.contains("HDR10Plus"))
    }

    @Test
    fun `should add dovi ranges only for decodable profiles`() {
        val doViDisplay =
            sdrCapabilities().copy(
                displayHdr10 = true,
                displayHlg = true,
                displayDolbyVision = true,
                dolbyVisionProfiles = setOf(8),
            )

        val ranges = hevcRangeTypes(tvDeviceProfile(doViDisplay))
        val profileFiveRanges =
            hevcRangeTypes(tvDeviceProfile(doViDisplay.copy(dolbyVisionProfiles = setOf(5, 8))))

        assertTrue(ranges.containsAll(setOf("DOVIWithHDR10", "DOVIWithHLG", "DOVIWithSDR")))
        assertFalse(ranges.contains("DOVIWithHDR10Plus"))
        assertFalse(ranges.contains("DOVI"))
        assertFalse(ranges.contains("DOVIWithEL"))
        assertTrue(profileFiveRanges.contains("DOVI"))
    }

    @Test
    fun `should limit h264 to sdr`() {
        val profile =
            tvDeviceProfile(
                sdrCapabilities().copy(
                    displayHdr10 = true,
                    displayDolbyVision = true,
                    dolbyVisionProfiles = setOf(5, 7, 8),
                ),
            )

        assertEquals(setOf("SDR"), rangeTypes(profile, "h264"))
    }

    @Test
    fun `should keep hls transcoding profile fields`() {
        val transcoding =
            tvDeviceProfile(sdrCapabilities())
                .jsonObject
                .getValue("TranscodingProfiles")
                .jsonArray
                .single()
                .jsonObject

        assertEquals("hls", transcoding.getValue("Protocol").jsonPrimitive.content)
        assertEquals("mp4", transcoding.getValue("Container").jsonPrimitive.content)
        assertEquals("1", transcoding.getValue("MinSegments").jsonPrimitive.content)
        assertEquals(
            true,
            transcoding
                .getValue("BreakOnNonKeyFrames")
                .jsonPrimitive.content
                .toBoolean(),
        )
    }

    @Test
    fun `should direct play only probed video codecs`() {
        val profile =
            tvDeviceProfile(
                sdrCapabilities().copy(
                    videoDecoders =
                        listOf(
                            decoder("h264"),
                            decoder("hevc"),
                        ),
                ),
            )
        val videoCodecs =
            profile
                .jsonObject
                .getValue("DirectPlayProfiles")
                .jsonArray
                .single()
                .jsonObject
                .getValue("VideoCodec")
                .jsonPrimitive
                .content

        assertEquals(setOf("h264", "hevc"), videoCodecs.split(",").toSet())
    }

    @Test
    fun `should retain avi direct play through bundled extractor`() {
        val containers =
            tvDeviceProfile(sdrCapabilities())
                .getValue("DirectPlayProfiles")
                .jsonArray
                .single()
                .jsonObject
                .getValue("Container")
                .jsonPrimitive
                .content
                .split(",")

        assertTrue("avi" in containers)
    }

    @Test
    fun `should serialize decoder and audio limits`() {
        val profile = tvDeviceProfile(sdrCapabilities())
        val hevcConditions = codecConditions(profile, type = "Video", codec = "hevc")
        val eac3Conditions = codecConditions(profile, type = "Audio", codec = "eac3")

        assertEquals("3840", hevcConditions.getValue("Width"))
        assertEquals("2160", hevcConditions.getValue("Height"))
        assertEquals("60", hevcConditions.getValue("VideoFramerate"))
        assertEquals("35000000", hevcConditions.getValue("VideoBitrate"))
        assertEquals("8", eac3Conditions.getValue("AudioChannels"))
    }

    @Test
    fun `should declare subtitle profiles`() {
        val profiles =
            tvDeviceProfile(sdrCapabilities())
                .jsonObject
                .getValue("SubtitleProfiles")
                .jsonArray
                .map(kotlinx.serialization.json.JsonElement::jsonObject)
                .associate {
                    it.getValue("Format").jsonPrimitive.content to it.getValue("Method").jsonPrimitive.content
                }

        assertEquals(
            mapOf(
                "subrip" to "External",
                "ass" to "Encode",
                "ssa" to "Encode",
                "pgssub" to "Embed",
                "vtt" to "Hls",
            ),
            profiles,
        )
    }

    private fun sdrCapabilities(): TvMediaCapabilities =
        TvMediaCapabilities(
            videoDecoders =
                listOf(
                    decoder("h264"),
                    decoder("hevc"),
                    decoder("vp9"),
                    decoder("av1"),
                ),
            dolbyVisionProfiles = emptySet(),
            displayDolbyVision = false,
            displayHdr10 = false,
            displayHlg = false,
            displayHdr10Plus = false,
            audioPassthroughCodecs = emptySet(),
            maxAudioChannels = 8,
        )

    private fun decoder(codec: String): VideoDecoderCapability =
        VideoDecoderCapability(
            codec = codec,
            maxWidth = 3_840,
            maxHeight = 2_160,
            maxFrameRate = 60,
            maxBitrate = 35_000_000,
        )

    private fun directAudioCodecs(profile: kotlinx.serialization.json.JsonObject): Set<String> =
        profile.jsonObject
            .getValue("DirectPlayProfiles")
            .jsonArray
            .single()
            .jsonObject
            .getValue("AudioCodec")
            .jsonPrimitive
            .content
            .split(",")
            .toSet()

    private fun hevcRangeTypes(profile: kotlinx.serialization.json.JsonObject): Set<String> =
        rangeTypes(profile, "hevc")

    private fun rangeTypes(
        profile: kotlinx.serialization.json.JsonObject,
        codec: String,
    ): Set<String> {
        val conditions =
            profile.jsonObject
                .getValue("CodecProfiles")
                .jsonArray
                .map(kotlinx.serialization.json.JsonElement::jsonObject)
                .single { it.getValue("Codec").jsonPrimitive.content == codec }
                .getValue("Conditions")
                .jsonArray
        return conditions
            .map(kotlinx.serialization.json.JsonElement::jsonObject)
            .single { it.getValue("Property").jsonPrimitive.content == "VideoRangeType" }
            .getValue("Value")
            .jsonPrimitive
            .content
            .split("|")
            .toSet()
    }

    private fun codecConditions(
        profile: kotlinx.serialization.json.JsonObject,
        type: String,
        codec: String,
    ): Map<String, String> =
        profile
            .getValue("CodecProfiles")
            .jsonArray
            .map(kotlinx.serialization.json.JsonElement::jsonObject)
            .single {
                it.getValue("Type").jsonPrimitive.content == type &&
                    it.getValue("Codec").jsonPrimitive.content == codec
            }.getValue("Conditions")
            .jsonArray
            .map(kotlinx.serialization.json.JsonElement::jsonObject)
            .associate {
                it.getValue("Property").jsonPrimitive.content to it.getValue("Value").jsonPrimitive.content
            }
}
