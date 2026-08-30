package app.homeflix.tv.feature.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VideoSourceFactoryTest {
    @Test
    fun `should build static direct url with pipeline params`() {
        val source =
            videoSource(
                baseUrl = "http://server.test:8096",
                playback = releasedPlayback(playMethod = PlayMethod.DIRECT_PLAY),
            )

        assertFalse(source.hls)
        assertTrue(source.url.startsWith("http://server.test:8096/Videos/item-1/stream?"))
        val query =
            source.url
                .substringAfter("?")
                .split("&")
                .associate { it.substringBefore("=") to it.substringAfter("=") }
        assertEquals("true", query["Static"])
        assertEquals("source-1", query["MediaSourceId"])
        assertEquals("session-1", query["PlaySessionId"])
        assertEquals("1", query["AudioStreamIndex"])
        assertEquals("3", query["SubtitleStreamIndex"])
        assertEquals("native-abc", query["PlaybackPipelineId"])
        assertEquals("native-abc-a1", query["PlaybackAttemptId"])
    }

    @Test
    fun `should mark transcoding url as hls`() {
        val source =
            videoSource(
                baseUrl = "http://server.test:8096",
                playback =
                    releasedPlayback(
                        playMethod = PlayMethod.TRANSCODE,
                        transcodingUrl = "/videos/item-1/main.m3u8?DeviceId=x",
                    ),
            )

        assertTrue(source.hls)
        assertEquals("http://server.test:8096/videos/item-1/main.m3u8?DeviceId=x", source.url)
    }

    @Test
    fun `should reject transcode without url`() {
        assertThrows(IllegalStateException::class.java) {
            videoSource(
                baseUrl = "http://server.test:8096",
                playback = releasedPlayback(playMethod = PlayMethod.TRANSCODE, transcodingUrl = null),
            )
        }
    }

    private fun releasedPlayback(
        playMethod: PlayMethod,
        transcodingUrl: String? = null,
    ): ReleasedPlayback =
        ReleasedPlayback(
            itemId = "item-1",
            mediaSourceId = "source-1",
            playSessionId = "session-1",
            playMethod = playMethod,
            audioStreamIndex = 1,
            subtitleStreamIndex = 3,
            pipelineId = "native-abc",
            attemptId = "native-abc-a1",
            transcodingUrl = transcodingUrl,
            transcodingSubProtocol = if (playMethod == PlayMethod.TRANSCODE) "hls" else null,
        )
}
