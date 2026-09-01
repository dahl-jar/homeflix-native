package app.homeflix.tv.feature.player

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlaybackTelemetryTest {
    @Test
    fun `should send queued events in order`() =
        runTest {
            val gateway = FlakyTelemetryGateway(failuresBeforeSuccess = 0)
            val telemetry = telemetry(gateway, backgroundScope)

            telemetry.log("playback_started", mapOf("videoCurrentTime" to 1))
            telemetry.log("playback_paused", mapOf("videoCurrentTime" to 2))
            telemetry.flush()

            assertEquals(
                listOf("playback_started", "playback_paused"),
                gateway.sent.map { it.getValue("event").jsonPrimitive.content },
            )
        }

    @Test
    fun `should retry three times then drop`() =
        runTest {
            val gateway = FlakyTelemetryGateway(failuresBeforeSuccess = Int.MAX_VALUE)
            val telemetry = telemetry(gateway, backgroundScope)

            telemetry.log("playback_started", emptyMap())
            telemetry.flush()

            assertEquals(3, gateway.attempts)
            assertTrue(gateway.sent.isEmpty())
            assertEquals(1, telemetry.droppedCount)
        }

    @Test
    fun `should drop after queue limit`() =
        runTest {
            val gateway = FlakyTelemetryGateway(failuresBeforeSuccess = 0)
            val telemetry =
                PlaybackTelemetry(
                    gateway = gateway,
                    pipeline = pipelineIds(),
                    scope = backgroundScope,
                    queueLimit = 1,
                )

            assertTrue(telemetry.log("kept", emptyMap()))
            assertFalse(telemetry.log("dropped", emptyMap()))
            telemetry.flush()

            assertEquals(1, telemetry.droppedCount)
        }

    private fun telemetry(
        gateway: TelemetryGateway,
        scope: kotlinx.coroutines.CoroutineScope,
    ): PlaybackTelemetry =
        PlaybackTelemetry(
            gateway = gateway,
            pipeline = pipelineIds(),
            scope = scope,
        )

    private fun pipelineIds(): PlaybackPipelineIds =
        PlaybackPipelineIds(
            itemId = "item-1",
            itemName = "Item",
            pipelineId = "native-abc",
            now = { 0L },
        )
}

private class FlakyTelemetryGateway(
    private val failuresBeforeSuccess: Int,
) : TelemetryGateway {
    val sent = mutableListOf<JsonObject>()
    var attempts = 0

    override suspend fun logPipelineEvent(payload: JsonObject) {
        attempts += 1
        if (attempts <= failuresBeforeSuccess) error("telemetry rejected")
        sent.add(payload)
    }
}
