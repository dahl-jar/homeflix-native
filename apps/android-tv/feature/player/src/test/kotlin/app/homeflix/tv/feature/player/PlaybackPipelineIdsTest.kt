package app.homeflix.tv.feature.player

import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PlaybackPipelineIdsTest {
    @Test
    fun `should reject invalid pipeline ids`() {
        assertThrows(IllegalArgumentException::class.java) { pipeline(id = "") }
        assertThrows(IllegalArgumentException::class.java) { pipeline(id = "has space") }
        assertThrows(IllegalArgumentException::class.java) { pipeline(id = "x".repeat(65)) }
    }

    @Test
    fun `should number attempts within the pipeline`() {
        val pipeline = pipeline(id = "native-abc")

        val first = pipeline.startAttempt()
        val second = pipeline.startAttempt()

        assertEquals("native-abc-a1", first.attemptId)
        assertEquals("native-abc-a2", second.attemptId)
        assertEquals(2, second.attempt)
    }

    @Test
    fun `should build sequenced event envelopes`() {
        var clock = 1_000L
        val pipeline = pipeline(id = "native-abc", now = { clock })
        val attempt = pipeline.startAttempt()
        pipeline.selectAttemptSource("source-9")
        clock = 1_250L

        val event = pipeline.nextEvent("source_selected", mapOf("sourceName" to "Remux"))

        assertEquals("source_selected", event.getValue("event").jsonPrimitive.content)
        assertEquals("native-abc", event.getValue("pipelineId").jsonPrimitive.content)
        assertEquals(attempt.attemptId, event.getValue("attemptId").jsonPrimitive.content)
        assertEquals("1", event.getValue("sequence").jsonPrimitive.content)
        assertEquals("native", event.getValue("component").jsonPrimitive.content)
        assertEquals("item-1", event.getValue("itemId").jsonPrimitive.content)
        assertEquals("source-9", event.getValue("mediaSourceId").jsonPrimitive.content)
        assertEquals("250", event.getValue("elapsedMs").jsonPrimitive.content)
        assertEquals("Remux", event.getValue("sourceName").jsonPrimitive.content)
        assertEquals(
            "2",
            pipeline
                .nextEvent("next")
                .getValue("sequence")
                .jsonPrimitive.content,
        )
    }

    private fun pipeline(
        id: String,
        now: () -> Long = { 0L },
    ): PlaybackPipelineIds =
        PlaybackPipelineIds(
            itemId = "item-1",
            itemName = "Item",
            pipelineId = id,
            now = now,
        )
}
