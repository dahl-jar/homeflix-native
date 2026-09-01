package app.homeflix.tv.feature.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PlaybackFormatTelemetryTest {
    @Test
    fun `should classify dolby vision pq video from actual format`() {
        val format =
            Format
                .Builder()
                .setSampleMimeType(MimeTypes.VIDEO_DOLBY_VISION)
                .setCodecs("dvh1.08.06")
                .setWidth(3_840)
                .setHeight(1_608)
                .setAverageBitrate(25_000_000)
                .setFrameRate(24f)
                .build()

        val fields = playbackFormatTelemetry(trackType = "video", format = format)

        assertEquals("video/dolby-vision", fields.getValue("sampleMimeType"))
        assertTrue(fields.getValue("dolbyVision") as Boolean)
        assertEquals("dvh1.08.06", fields.getValue("codecs"))
        assertEquals(3_840, fields.getValue("width"))
        assertEquals(1_608, fields.getValue("height"))
        assertEquals(25_000_000, fields.getValue("bitrate"))
        assertEquals(24f, fields.getValue("frameRate"))
        assertTrue(fields.getValue("hdr") as Boolean)
    }

    @Test
    fun `should classify eac3 joc as atmos`() {
        val joc =
            Format
                .Builder()
                .setSampleMimeType(MimeTypes.AUDIO_E_AC3_JOC)
                .setChannelCount(8)
                .build()
        val fields = playbackFormatTelemetry("audio", joc)

        assertTrue(fields.getValue("atmos") as Boolean)
        assertEquals(8, fields.getValue("channelCount"))
    }

    @Test
    fun `should classify aac as non atmos`() {
        val aac =
            Format
                .Builder()
                .setSampleMimeType(MimeTypes.AUDIO_AAC)
                .setChannelCount(6)
                .setSampleRate(48_000)
                .build()
        val fields = playbackFormatTelemetry("audio", aac)

        assertFalse(fields.getValue("atmos") as Boolean)
        assertEquals(48_000, fields.getValue("sampleRate"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["dvh1.08.06", " DVHE.05.06 "])
    fun `should classify dolby vision from codec identifier`(codecs: String) {
        val format =
            Format
                .Builder()
                .setSampleMimeType(MimeTypes.VIDEO_H265)
                .setCodecs(codecs)
                .build()

        val fields = playbackFormatTelemetry("video", format)

        assertTrue(fields.getValue("dolbyVision") as Boolean)
        assertTrue(fields.getValue("hdr") as Boolean)
    }

    @Test
    fun `should classify pq transfer as hdr`() {
        val fields =
            playbackFormatTelemetry(
                trackType = "video",
                format = Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H265).build(),
                colorTransferOverride = C.COLOR_TRANSFER_ST2084,
            )

        assertTrue(fields.getValue("hdr") as Boolean)
    }

    @Test
    fun `should classify hlg transfer as hdr`() {
        val fields =
            playbackFormatTelemetry(
                trackType = "video",
                format = Format.Builder().setSampleMimeType(MimeTypes.VIDEO_H265).build(),
                colorTransferOverride = C.COLOR_TRANSFER_HLG,
            )

        assertTrue(fields.getValue("hdr") as Boolean)
    }

    @Test
    fun `should classify standard hevc as sdr`() {
        val format =
            Format
                .Builder()
                .setSampleMimeType(MimeTypes.VIDEO_H265)
                .setCodecs("hvc1.1.6.L150")
                .build()

        val fields = playbackFormatTelemetry("video", format, colorTransferOverride = 0)

        assertFalse(fields.getValue("dolbyVision") as Boolean)
        assertFalse(fields.getValue("hdr") as Boolean)
    }

    @Test
    fun `should omit zero format dimensions and rates`() {
        val format =
            Format
                .Builder()
                .setWidth(0)
                .setHeight(0)
                .setAverageBitrate(0)
                .setFrameRate(0f)
                .setChannelCount(0)
                .setSampleRate(0)
                .build()

        val fields = playbackFormatTelemetry("unknown", format)

        listOf("width", "height", "bitrate", "frameRate", "channelCount", "sampleRate").forEach { key ->
            assertFalse(fields.containsKey(key))
        }
    }
}
