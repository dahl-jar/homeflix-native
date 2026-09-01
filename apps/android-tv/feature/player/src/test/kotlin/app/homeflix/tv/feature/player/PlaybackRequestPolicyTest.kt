package app.homeflix.tv.feature.player

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlaybackRequestPolicyTest {
    @Test
    fun `should disable direct stream during discovery`() {
        val policy = androidTvPlaybackPolicy()

        assertTrue(policy.enableDirectPlay)
        assertFalse(policy.enableDirectStream)
        assertTrue(policy.enableTranscoding)
        assertTrue(policy.allowVideoStreamCopy)
        assertTrue(policy.allowAudioStreamCopy)
    }

    @Test
    fun `should force hls for remote source`() {
        val policy = androidTvPlaybackPolicy(mediaSource(isRemote = true))

        assertFalse(policy.enableDirectPlay)
        assertFalse(policy.enableDirectStream)
        assertTrue(policy.enableTranscoding)
    }

    @Test
    fun `should retain exact direct play for local source`() {
        val policy = androidTvPlaybackPolicy(mediaSource(isRemote = false))

        assertTrue(policy.enableDirectPlay)
        assertFalse(policy.enableDirectStream)
        assertTrue(policy.enableTranscoding)
    }

    @Test
    fun `should force hls when source locality is unknown`() {
        val policy = androidTvPlaybackPolicy(mediaSource(isRemote = null))

        assertFalse(policy.enableDirectPlay)
        assertFalse(policy.enableDirectStream)
    }

    private fun mediaSource(isRemote: Boolean?): MediaSourceDto =
        MediaSourceDto(
            id = "source-1",
            name = "Source",
            supportsDirectPlay = true,
            supportsDirectStream = true,
            supportsTranscoding = true,
            transcodingUrl = "/Videos/item-1/master.m3u8",
            transcodingSubProtocol = "hls",
            mediaStreams = emptyList(),
            isRemote = isRemote,
        )
}
