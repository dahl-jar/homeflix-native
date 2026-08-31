package app.homeflix.tv.feature.player

import androidx.media3.common.PlaybackException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayerErrorDetailsTest {
    @Test
    fun `should expose renderer format and media codec diagnostics`() {
        val details =
            playerErrorDetails(
                PlayerErrorSnapshot(
                    reason = "ERROR_CODE_DECODING_FAILED",
                    errorType = "MediaCodecDecoderException",
                    errorCode = PlaybackException.ERROR_CODE_DECODING_FAILED,
                    rendererName = "MediaCodecVideoRenderer",
                    rendererIndex = 0,
                    formatSupport = 3,
                    mimeType = "video/hevc",
                    codecs = "hvc1.2.4.L153.B0",
                    width = 1920,
                    height = 1080,
                    decoderName = "c2.android.hevc.decoder",
                    decoderDiagnostic = "android.media.MediaCodec.error_neg_2147483648",
                    decoderErrorCode = -2_147_483_648,
                ),
            )

        assertEquals("ERROR_CODE_DECODING_FAILED", details.reason)
        assertEquals("MediaCodecDecoderException", details.telemetry["errorType"])
        assertEquals(PlaybackException.ERROR_CODE_DECODING_FAILED, details.telemetry["errorCode"])
        assertEquals("ERROR_CODE_DECODING_FAILED", details.telemetry["errorName"])
        assertEquals(1920, details.telemetry["videoWidth"])
        assertEquals(1080, details.telemetry["videoHeight"])
        val message = details.telemetry.getValue("errorMessage").toString()
        assertTrue(message.contains("renderer=MediaCodecVideoRenderer"))
        assertTrue(message.contains("rendererIndex=0"))
        assertTrue(message.contains("formatSupport=3"))
        assertTrue(message.contains("mime=video/hevc"))
        assertTrue(message.contains("codecs=hvc1.2.4.L153.B0"))
        assertTrue(message.contains("size=1920x1080"))
        assertTrue(message.contains("decoder=c2.android.hevc.decoder"))
        assertTrue(message.contains("diagnostic=android.media.MediaCodec.error_neg_2147483648"))
        assertTrue(message.contains("codecError=-2147483648"))
    }
}
