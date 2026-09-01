package app.homeflix.tv.feature.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import app.homeflix.tv.core.designsystem.HomeflixTheme
import kotlinx.coroutines.awaitCancellation
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shouldShowPipelineStagesFromGatewayEvents() {
        val gateway =
            StageEmittingGateway(
                listOf(
                    stageEvent(sequence = 1, stageId = "sources", label = "Finding sources", status = "complete"),
                    stageEvent(sequence = 2, stageId = "resolve", label = "Resolving source", status = "active"),
                ),
            )

        composeRule.setContent {
            HomeflixTheme {
                PlayerScreen(
                    gateway = gateway,
                    baseUrl = "http://server",
                    userId = "user-1",
                    itemId = "item-1",
                    onExit = {},
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Resolving source")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Movie Title").assertIsDisplayed()
        composeRule.onNodeWithText("Finding sources").assertIsDisplayed()
        composeRule.onNodeWithText("PREPARING PLAYBACK").assertIsDisplayed()

        val rootHeight =
            composeRule
                .onRoot()
                .fetchSemanticsNode()
                .size.height
        val eyebrowTop =
            composeRule
                .onNodeWithText("PREPARING PLAYBACK")
                .fetchSemanticsNode()
                .positionInRoot.y
        val centeringDetail = "eyebrowTop=" + eyebrowTop + " rootHeight=" + rootHeight
        assertTrue(
            "pipeline content must be vertically centered ($centeringDetail)",
            eyebrowTop > rootHeight / 4f,
        )
    }

    @Test
    fun shouldHideOverlayWhenPlaying() {
        composeRule.setContent {
            HomeflixTheme {
                PipelineOverlay(
                    itemName = "Movie Title",
                    backdropUrl = null,
                    progress = PipelineProgress(visible = false, videoVisible = true),
                )
            }
        }

        assertEquals(0, composeRule.onAllNodes(hasText("PREPARING PLAYBACK")).fetchSemanticsNodes().size)
    }

    @Test
    fun shouldMoveFocusAcrossBottomActionsWithDpad() {
        composeRule.setContent {
            HomeflixTheme {
                PlayerControlsPanel(
                    item = movieItem(),
                    snapshot = playingSnapshot(),
                    videoContentMode = VideoContentMode.FIT,
                    callbacks =
                        PlayerActionCallbacks(
                            onExit = {},
                            onTogglePlay = {},
                            onSeekBy = {},
                            onToggleVideoContentMode = {},
                            onOpenAudioMenu = {},
                            onOpenSubtitleMenu = {},
                            onOpenEpisodes = null,
                            onPlayNext = null,
                        ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Pause").performKeyInput {
            pressKey(Key.DirectionLeft)
        }
        composeRule.onNodeWithContentDescription("Rewind 10 seconds").assertIsFocused()
        composeRule.onNodeWithContentDescription("Rewind 10 seconds").performKeyInput {
            pressKey(Key.DirectionRight)
        }
        composeRule.onNodeWithContentDescription("Pause").assertIsFocused()
        composeRule.onNodeWithContentDescription("Pause").performKeyInput {
            pressKey(Key.DirectionDown)
        }
        if (isFocused("Subtitles")) {
            composeRule.onNodeWithContentDescription("Subtitles").performKeyInput {
                pressKey(Key.DirectionLeft)
            }
        }
        composeRule.onNodeWithContentDescription("Audio").assertIsFocused()
        composeRule.onNodeWithContentDescription("Audio").performKeyInput {
            pressKey(Key.DirectionRight)
        }
        composeRule.onNodeWithContentDescription("Subtitles").assertIsFocused()
    }

    @Test
    fun shouldToggleVideoFitAction() {
        composeRule.setContent {
            var videoContentMode by remember { mutableStateOf(VideoContentMode.FIT) }
            HomeflixTheme {
                PlayerControlsPanel(
                    item = movieItem(),
                    snapshot = playingSnapshot(),
                    videoContentMode = videoContentMode,
                    callbacks =
                        PlayerActionCallbacks(
                            onExit = {},
                            onTogglePlay = {},
                            onSeekBy = {},
                            onToggleVideoContentMode = { videoContentMode = videoContentMode.next() },
                            onOpenAudioMenu = {},
                            onOpenSubtitleMenu = {},
                            onOpenEpisodes = null,
                            onPlayNext = null,
                        ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Fill screen").performClick()
        composeRule.onNodeWithContentDescription("Fit video").assertIsDisplayed()
    }

    private fun isFocused(description: String): Boolean =
        composeRule
            .onNodeWithContentDescription(description)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Focused) == true

    private fun movieItem(): PlayableItem =
        PlayableItem(
            id = "item-1",
            name = "Movie Title",
            type = "Movie",
            seriesId = null,
            seriesName = null,
            indexNumber = null,
            parentIndexNumber = null,
            isMissing = false,
            resumePositionTicks = 0,
            backdropUrl = null,
        )

    private fun playingSnapshot(): PlaybackSnapshot =
        PlaybackSnapshot(
            status = PlaybackStatus.PLAYING,
            positionSeconds = 30.0,
            durationSeconds = 120.0,
            bufferedSeconds = 60.0,
            tracks =
                TrackCatalog(
                    audioTracks =
                        listOf(
                            playbackTrack(label = "English · 5.1", streamIndex = 1, typeOrdinal = 0),
                            playbackTrack(label = "Norsk · Stereo", streamIndex = 2, typeOrdinal = 1),
                        ),
                    subtitleTracks =
                        listOf(playbackTrack(label = "English", streamIndex = 3, typeOrdinal = 0)),
                    selectedAudioTrack = null,
                    selectedSubtitleTrack = null,
                ),
            pipeline = PipelineProgress(visible = false, videoVisible = true),
            reason = null,
        )

    private fun playbackTrack(
        label: String,
        streamIndex: Int,
        typeOrdinal: Int,
    ): PlaybackTrack =
        PlaybackTrack(
            label = label,
            language = null,
            streamIndex = streamIndex,
            typeOrdinal = typeOrdinal,
            isExternal = false,
        )

    private fun stageEvent(
        sequence: Long,
        stageId: String,
        label: String,
        status: String,
    ): PipelineEventDto =
        PipelineEventDto(
            sequence = sequence,
            stageId = stageId,
            label = label,
            order = sequence.toInt() * 100,
            status = status,
            sourceAttempt = null,
            sourceCount = null,
            reason = null,
        )
}

private class StageEmittingGateway(
    private val events: List<PipelineEventDto>,
) : PlayerGateway {
    override suspend fun resolveAttempt(request: ResolveRequest): PlaybackInfoResult = awaitCancellation()

    override suspend fun releaseSource(request: ReleaseRequest): PlaybackInfoResult = awaitCancellation()

    override suspend fun pipelineProgress(
        pipelineId: String,
        attemptId: String,
        afterSequence: Long,
    ): List<PipelineEventDto> = events.filter { it.sequence > afterSequence }

    override suspend fun reportStart(
        context: SessionContext,
        snapshot: SessionSnapshot,
    ) = Unit

    override suspend fun reportProgress(
        context: SessionContext,
        snapshot: SessionSnapshot,
    ) = Unit

    override suspend fun reportStop(
        context: SessionContext,
        snapshot: SessionSnapshot,
    ) = Unit

    override suspend fun logPipelineEvent(payload: JsonObject) = Unit

    override suspend fun fetchItem(
        userId: String,
        itemId: String,
    ): PlayableItem =
        PlayableItem(
            id = itemId,
            name = "Movie Title",
            type = "Movie",
            seriesId = null,
            seriesName = null,
            indexNumber = null,
            parentIndexNumber = null,
            isMissing = false,
            resumePositionTicks = 0,
            backdropUrl = null,
        )

    override suspend fun nextUpEpisode(
        userId: String,
        seriesId: String,
    ): PlayableItem? = null

    override suspend fun followingEpisodes(
        userId: String,
        seriesId: String,
        itemId: String,
    ): List<PlayableItem> = emptyList()

    override suspend fun seriesEpisodes(
        userId: String,
        seriesId: String,
        itemId: String,
    ): List<PlayableItem> = emptyList()

    override suspend fun mediaSegments(itemId: String): List<SegmentDto> = emptyList()
}
