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
        assertEquals(TimelineMode.ABSOLUTE_ITEM, source.timelineMode)
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
        assertEquals(TimelineMode.ABSOLUTE_ITEM, source.timelineMode)
        assertEquals("http://server.test:8096/videos/item-1/main.m3u8?DeviceId=x", source.url)
    }

    @Test
    fun `should map preseeked progressive time to canonical item time`() {
        val source =
            videoSource(
                baseUrl = "http://server.test:8096",
                playback =
                    releasedPlayback(
                        playMethod = PlayMethod.DIRECT_STREAM,
                        transcodingUrl = "/videos/item-1/remux.mkv",
                    ),
            )
        val timeline = source.timeline(itemStartSeconds = 11_557.424)

        assertEquals(TimelineMode.SERVER_PRESEEKED, source.timelineMode)
        assertEquals(11_557.424, timeline.originSeconds)
        assertEquals(0.0, timeline.playerPositionSeconds(11_557.424))
        assertEquals(600.0, timeline.playerPositionSeconds(12_157.424))
        assertEquals(12_157.424, timeline.itemPositionSeconds(600.0))
        assertEquals(0.0, timeline.itemDurationSeconds(0.0))
        assertEquals(14_535.424, timeline.itemDurationSeconds(2_978.0))
    }

    @Test
    fun `should keep absolute timeline positions unchanged`() {
        val timeline = PlaybackTimeline(TimelineMode.ABSOLUTE_ITEM, originSeconds = 500.0)

        assertEquals(600.0, timeline.playerPositionSeconds(600.0))
        assertEquals(600.0, timeline.itemPositionSeconds(600.0))
        assertEquals(1_000.0, timeline.itemDurationSeconds(1_000.0))
    }

    @Test
    fun `should select direct stream before transcode`() {
        val source = mediaSource(supportsDirectStream = true, supportsTranscoding = true)

        assertEquals(PlayMethod.DIRECT_STREAM, playbackMethod(source))
    }

    @Test
    fun `should select transcode when stream copy is unavailable`() {
        val source = mediaSource(supportsDirectStream = false, supportsTranscoding = true)

        assertEquals(PlayMethod.TRANSCODE, playbackMethod(source))
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

    private fun mediaSource(
        supportsDirectStream: Boolean,
        supportsTranscoding: Boolean,
    ): MediaSourceDto =
        MediaSourceDto(
            id = "source-1",
            name = "Source",
            supportsDirectPlay = false,
            supportsDirectStream = supportsDirectStream,
            supportsTranscoding = supportsTranscoding,
            transcodingUrl = null,
            transcodingSubProtocol = null,
            mediaStreams = emptyList(),
        )
}
