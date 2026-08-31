package app.homeflix.tv.feature.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NextEpisodeTest {
    @Test
    fun `should pick next non missing episode after current`() {
        val episodes =
            listOf(
                episode("e1"),
                episode("e2", missing = true),
                episode("e3"),
            )

        assertEquals("e3", selectFollowingEpisode(episodes, currentItemId = "e1")?.id)
        assertEquals("e3", selectFollowingEpisode(episodes, currentItemId = "e2")?.id)
        assertNull(selectFollowingEpisode(episodes, currentItemId = "e3"))
        assertNull(selectFollowingEpisode(episodes, currentItemId = "unknown"))
    }

    @Test
    fun `should trigger countdown on outro`() {
        val advanced = mutableListOf<PlayableItem>()
        val countdown = NextEpisodeCountdown(itemId = "e1", nextEpisode = episode("e2"), onAdvance = advanced::add)

        val idle = countdown.update(activeSegmentOutroId = null, ended = false)
        val active = countdown.update(activeSegmentOutroId = "outro-1", ended = false)

        assertFalse(idle.active)
        assertTrue(active.active)
        assertEquals(10, active.remainingSeconds)
    }

    @Test
    fun `should advance once when countdown reaches zero`() {
        val advanced = mutableListOf<PlayableItem>()
        val countdown = NextEpisodeCountdown(itemId = "e1", nextEpisode = episode("e2"), onAdvance = advanced::add)
        countdown.update(activeSegmentOutroId = null, ended = true)

        repeat(12) { countdown.tick() }

        assertEquals(listOf("e2"), advanced.map(PlayableItem::id))
        assertEquals(0, countdown.update(activeSegmentOutroId = null, ended = true).remainingSeconds)
    }

    @Test
    fun `should not advance before countdown ends`() {
        val advanced = mutableListOf<PlayableItem>()
        val countdown = NextEpisodeCountdown(itemId = "e1", nextEpisode = episode("e2"), onAdvance = advanced::add)
        countdown.update(activeSegmentOutroId = "outro-1", ended = false)

        repeat(5) { countdown.tick() }

        assertTrue(advanced.isEmpty())
        assertEquals(5, countdown.update(activeSegmentOutroId = "outro-1", ended = false).remainingSeconds)
    }

    @Test
    fun `should cancel countdown without advancing`() {
        val advanced = mutableListOf<PlayableItem>()
        val countdown = NextEpisodeCountdown(itemId = "e1", nextEpisode = episode("e2"), onAdvance = advanced::add)
        countdown.update(activeSegmentOutroId = "outro-1", ended = false)

        countdown.cancel()
        repeat(12) { countdown.tick() }

        assertFalse(countdown.update(activeSegmentOutroId = "outro-1", ended = false).active)
        assertTrue(advanced.isEmpty())
    }

    @Test
    fun `should stay idle without next episode`() {
        val countdown = NextEpisodeCountdown(itemId = "e1", nextEpisode = null, onAdvance = {})

        assertFalse(countdown.update(activeSegmentOutroId = "outro-1", ended = false).active)
    }

    @Test
    fun `should play next immediately once`() {
        val advanced = mutableListOf<PlayableItem>()
        val countdown = NextEpisodeCountdown(itemId = "e1", nextEpisode = episode("e2"), onAdvance = advanced::add)

        countdown.playNext()
        countdown.playNext()

        assertEquals(1, advanced.size)
    }

    private fun episode(
        id: String,
        missing: Boolean = false,
    ): PlayableItem =
        PlayableItem(
            id = id,
            name = "Episode $id",
            type = "Episode",
            seriesId = "series-1",
            seriesName = "Series",
            indexNumber = 1,
            parentIndexNumber = 1,
            isMissing = missing,
            resumePositionTicks = 0,
            backdropUrl = null,
        )
}
