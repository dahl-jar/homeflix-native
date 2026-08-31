package app.homeflix.tv.feature.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PipelineProgressTest {
    @Test
    fun `should add and order stages by order then id`() {
        val progress =
            PipelineProgress()
                .transition(stageEvent("resolve", order = 200, status = StageStatus.ACTIVE))
                .transition(stageEvent("sources", order = 100, status = StageStatus.COMPLETE))
                .transition(stageEvent("alpha", order = 200, status = StageStatus.PENDING))

        assertEquals(listOf("sources", "alpha", "resolve"), progress.stages.map(PipelineStage::id))
    }

    @Test
    fun `should replace stage on repeated id`() {
        val progress =
            PipelineProgress()
                .transition(stageEvent("resolve", order = 100, status = StageStatus.ACTIVE))
                .transition(stageEvent("resolve", order = 100, status = StageStatus.COMPLETE))

        assertEquals(1, progress.stages.size)
        assertEquals(StageStatus.COMPLETE, progress.stages.single().status)
    }

    @Test
    fun `should ignore stage event with blank id or label`() {
        val progress =
            PipelineProgress()
                .transition(stageEvent("", order = 100, status = StageStatus.ACTIVE))
                .transition(
                    PipelineEvent.StageProgress(
                        stageId = "resolve",
                        label = "",
                        order = 100,
                        status = StageStatus.ACTIVE,
                    ),
                )

        assertTrue(progress.stages.isEmpty())
    }

    @Test
    fun `should offset source attempt and track counts`() {
        val progress =
            PipelineProgress(sourceOffset = 2)
                .transition(
                    stageEvent("resolve", order = 100, status = StageStatus.ACTIVE)
                        .copy(sourceAttempt = 1, sourceCount = 4),
                )

        assertEquals(3, progress.sourceAttempt)
        assertEquals(4, progress.sourceCount)
    }

    @Test
    fun `should set reason on failed stage and clear it otherwise`() {
        val failed =
            PipelineProgress()
                .transition(
                    stageEvent("resolve", order = 100, status = StageStatus.FAILED)
                        .copy(reason = "probe failed"),
                )
        val recovered = failed.transition(stageEvent("resolve", order = 100, status = StageStatus.ACTIVE))

        assertEquals("probe failed", failed.reason)
        assertNull(recovered.reason)
    }

    @Test
    fun `should default source failure reason on failed stage`() {
        val progress =
            PipelineProgress()
                .transition(stageEvent("resolve", order = 100, status = StageStatus.FAILED))

        assertEquals("source failed", progress.reason)
    }

    @Test
    fun `should reset stages when resolution starts`() {
        val progress =
            PipelineProgress(attempt = 3, sourceOffset = 1)
                .transition(stageEvent("resolve", order = 100, status = StageStatus.ACTIVE))
                .transition(PipelineEvent.ResolutionStarted)

        assertTrue(progress.stages.isEmpty())
        assertEquals(3, progress.attempt)
        assertEquals(1, progress.sourceOffset)
    }

    @Test
    fun `should complete all stages when resolution completes`() {
        val progress =
            PipelineProgress()
                .transition(stageEvent("sources", order = 100, status = StageStatus.COMPLETE))
                .transition(stageEvent("resolve", order = 200, status = StageStatus.ACTIVE))
                .transition(PipelineEvent.ResolutionCompleted(sourceCount = 2))

        assertTrue(progress.stages.all { it.status == StageStatus.COMPLETE })
        assertEquals(2, progress.sourceCount)
    }

    @Test
    fun `should show video on release completion`() {
        val progress = PipelineProgress().transition(PipelineEvent.ReleaseCompleted)

        assertTrue(progress.videoVisible)
        assertTrue(progress.visible)
    }

    @Test
    fun `should hide overlay on playing`() {
        val progress =
            PipelineProgress()
                .transition(stageEvent("player", order = 100, status = StageStatus.ACTIVE))
                .transition(PipelineEvent.Playing)

        assertFalse(progress.visible)
        assertTrue(progress.videoVisible)
        assertTrue(progress.stages.all { it.status == StageStatus.COMPLETE })
    }

    @Test
    fun `should reset on retry keeping source position`() {
        val progress =
            PipelineProgress(attempt = 1, sourceOffset = 0)
                .transition(
                    stageEvent("resolve", order = 100, status = StageStatus.ACTIVE)
                        .copy(sourceAttempt = 2),
                ).transition(PipelineEvent.Retry)

        assertEquals(2, progress.attempt)
        assertEquals(2, progress.sourceOffset)
        assertTrue(progress.stages.isEmpty())
    }

    @Test
    fun `should retry from source offset when no source attempt was reported`() {
        val progress = PipelineProgress(attempt = 2, sourceOffset = 3).transition(PipelineEvent.Retry)

        assertEquals(3, progress.attempt)
        assertEquals(3, progress.sourceOffset)
    }

    @Test
    fun `should mark active stage failed on failed event`() {
        val progress =
            PipelineProgress()
                .transition(stageEvent("sources", order = 100, status = StageStatus.COMPLETE))
                .transition(stageEvent("resolve", order = 200, status = StageStatus.ACTIVE))
                .transition(PipelineEvent.Failed(reason = "player crashed"))

        assertEquals(StageStatus.FAILED, progress.stages.last().status)
        assertEquals(StageStatus.COMPLETE, progress.stages.first().status)
        assertEquals("player crashed", progress.reason)
        assertTrue(progress.visible)
        assertFalse(progress.videoVisible)
    }

    @Test
    fun `should mark first active stage failed`() {
        val progress =
            PipelineProgress()
                .transition(stageEvent("sources", order = 100, status = StageStatus.ACTIVE))
                .transition(PipelineEvent.Failed(reason = "boom"))

        assertEquals(StageStatus.FAILED, progress.stages.single().status)
    }

    @Test
    fun `should keep source attempt and count when stage event has none`() {
        val progress =
            PipelineProgress(sourceOffset = 2)
                .transition(
                    stageEvent("resolve", order = 100, status = StageStatus.ACTIVE)
                        .copy(sourceAttempt = 1, sourceCount = 4),
                ).transition(stageEvent("stream", order = 200, status = StageStatus.ACTIVE))

        assertEquals(3, progress.sourceAttempt)
        assertEquals(4, progress.sourceCount)
    }

    @Test
    fun `should default failure reason`() {
        val progress = PipelineProgress().transition(PipelineEvent.Failed(reason = null))

        assertEquals("playback failed", progress.reason)
    }

    private fun stageEvent(
        id: String,
        order: Int,
        status: StageStatus,
    ): PipelineEvent.StageProgress =
        PipelineEvent.StageProgress(
            stageId = id,
            label = "Stage $id",
            order = order,
            status = status,
        )
}
