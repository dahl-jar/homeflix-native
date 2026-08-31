package app.homeflix.tv.feature.player

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `should keep sdr and backward compatible dovi range without hdr display`() {
        val ranges = hevcRangeTypes(tvDeviceProfile(sdrCapabilities()))

        assertEquals(setOf("SDR", "DOVIWithSDR"), ranges)
    }

    @Test
    fun `should add hdr fallback ranges for hdr10 display without dolby vision`() {
        val ranges =
            hevcRangeTypes(tvDeviceProfile(sdrCapabilities().copy(displayHdr10 = true, displayHlg = true)))

        assertTrue(
            ranges.containsAll(
                setOf(
                    "HDR10",
                    "HDR10Plus",
                    "HLG",
                    "DOVIWithHDR10",
                    "DOVIWithHDR10Plus",
                    "DOVIWithEL",
                    "DOVIWithELHDR10Plus",
                    "DOVIWithHLG",
                ),
            ),
        )
        assertFalse(ranges.contains("DOVI"))
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

        assertTrue(ranges.containsAll(setOf("DOVIWithHDR10", "DOVIWithHLG", "DOVIWithSDR", "DOVIWithHDR10Plus")))
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
        val videoCodecs =
            tvDeviceProfile(sdrCapabilities().copy(videoCodecs = setOf("h264", "hevc")))
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
            videoCodecs = setOf("h264", "hevc", "vp9", "av1"),
            dolbyVisionProfiles = emptySet(),
            displayDolbyVision = false,
            displayHdr10 = false,
            displayHlg = false,
            displayHdr10Plus = false,
            audioPassthroughCodecs = emptySet(),
            maxAudioChannels = 8,
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
}
