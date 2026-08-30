package app.homeflix.tv.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvFocusAppearance
import app.homeflix.tv.core.designsystem.TvFocusSurface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AUTO_HIDE_DELAY_MS = 3_500L
private const val COUNTDOWN_TICK_MS = 1_000L
private const val TICKS_PER_SECOND = 10_000_000L
private val OVERLAY_MARGIN = 48.dp
private const val LOADING_TEXT_SIZE = 16
private val FAILURE_BUTTON_CORNER_RADIUS = 8.dp
private val FAILURE_BUTTON_HORIZONTAL_PADDING = 16.dp
private val FAILURE_BUTTON_VERTICAL_PADDING = 8.dp
private val FAILURE_STACK_SPACING = 24.dp
private val FAILURE_BUTTON_SPACING = 12.dp
private val FailureButtonText = Color(0xFF141414)

private sealed interface PlayerBootstrap {
    data object Loading : PlayerBootstrap

    data object Failed : PlayerBootstrap

    data class Ready(
        val item: PlayableItem,
    ) : PlayerBootstrap
}

@Composable
fun PlayerScreen(
    gateway: PlayerGateway,
    baseUrl: String,
    userId: String,
    itemId: String,
    onExit: () -> Unit,
) {
    var bootstrap by remember { mutableStateOf<PlayerBootstrap>(PlayerBootstrap.Loading) }
    var retryToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(itemId, retryToken) {
        bootstrap = PlayerBootstrap.Loading
        bootstrap =
            try {
                val item = gateway.fetchItem(userId, itemId)
                val playable =
                    if (item.type == "Series") {
                        gateway.nextUpEpisode(userId, item.id) ?: error("series has no playable episode")
                    } else {
                        item
                    }
                PlayerBootstrap.Ready(playable)
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                PlayerBootstrap.Failed
            }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (val current = bootstrap) {
            PlayerBootstrap.Loading -> {
                BackHandler(onBack = onExit)
                Text(
                    text = "Preparing playback",
                    color = HomeflixColors.Muted,
                    fontSize = LOADING_TEXT_SIZE.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            PlayerBootstrap.Failed -> {
                BackHandler(onBack = onExit)
                PlayerFailure(
                    reason = "Playback could not be prepared",
                    onRetry = { retryToken += 1 },
                    onBack = onExit,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is PlayerBootstrap.Ready ->
                key(current.item.id) {
                    PlayerPlayback(
                        gateway = gateway,
                        baseUrl = baseUrl,
                        userId = userId,
                        item = current.item,
                        onAdvance = { next -> bootstrap = PlayerBootstrap.Ready(next) },
                        onExit = onExit,
                    )
                }
        }
    }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
private fun PlayerPlayback(
    gateway: PlayerGateway,
    baseUrl: String,
    userId: String,
    item: PlayableItem,
    onAdvance: (PlayableItem) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = remember { ExoPlayer.Builder(context).build() }
    val runtime =
        remember {
            val deviceProfile = tvDeviceProfile(probeTvMediaCapabilities(context))
            val negotiator =
                PlaybackNegotiator(
                    gateway = gateway,
                    deviceProfile = deviceProfile,
                    baseUrl = baseUrl,
                    watcherFactory = PipelineProgressWatcher(gateway, scope),
                )
            val pipeline = negotiator.createPipeline(item)
            PlaybackRuntime(
                scope = scope,
                dependencies =
                    PlaybackRuntimeDependencies(
                        negotiate = negotiator::negotiate,
                        bindPlayer = ExoPlayerAdapter(player, scope)::bind,
                        createReporter = { reporterContext -> PlaybackSessionReporter(gateway, reporterContext) },
                        telemetry = PlaybackTelemetry(gateway = gateway, pipeline = pipeline, scope = scope),
                        pipeline = pipeline,
                    ),
                request =
                    PlaybackStartRequest(
                        item = item,
                        userId = userId,
                        startTimeTicks = item.resumePositionTicks,
                    ),
            )
        }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    LaunchedEffect(runtime) { runtime.start() }

    val snapshot by runtime.snapshot.collectAsState()
    val controls = remember { PlayerControlsState() }
    var menu by remember { mutableStateOf<TrackMenuKind?>(null) }
    var episodeMenuOpen by remember { mutableStateOf(false) }
    var seriesEpisodes by remember { mutableStateOf<List<PlayableItem>>(emptyList()) }

    val skip = rememberSkipSegments(gateway, item, snapshot, runtime)
    val nextEpisode =
        rememberNextEpisode(
            gateway = gateway,
            userId = userId,
            item = item,
            snapshot = snapshot,
            activeSegment = skip.activeSegment,
            onAdvance = onAdvance,
            runtime = runtime,
            scope = scope,
        )

    val controlsVisible = controls.visible(snapshot.status) && !snapshot.pipeline.visible
    val menuOpen = menu != null || episodeMenuOpen
    LaunchedEffect(menuOpen) { controls.pinned = menuOpen }
    LaunchedEffect(snapshot.status, controlsVisible, controls.pinned, controls.revision) {
        if (controlsVisible && controls.shouldAutoHide(snapshot.status)) {
            delay(AUTO_HIDE_DELAY_MS)
            controls.hide()
        }
    }

    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(controlsVisible, menuOpen) {
        if (!controlsVisible && !menuOpen) rootFocus.requestFocus()
    }

    val exit: () -> Unit = {
        scope.launch {
            runtime.stop()
            onExit()
        }
    }
    BackHandler {
        when {
            menuOpen -> {
                menu = null
                episodeMenuOpen = false
                controls.show()
            }
            controlsVisible && snapshot.status == PlaybackStatus.PLAYING -> controls.hide()
            else -> exit()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || controlsVisible || menuOpen) {
                        false
                    } else {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                runtime.seekBy(-PLAYER_SEEK_STEP_SECONDS)
                                true
                            }
                            Key.DirectionRight -> {
                                runtime.seekBy(PLAYER_SEEK_STEP_SECONDS)
                                true
                            }
                            Key.DirectionCenter, Key.Enter -> {
                                val activeSkip = skip.activeSegment
                                if (activeSkip != null) skip.skip() else controls.show()
                                true
                            }
                            Key.DirectionDown, Key.DirectionUp -> {
                                controls.show()
                                true
                            }
                            Key.MediaPlayPause -> {
                                if (snapshot.status == PlaybackStatus.PAUSED) runtime.play() else runtime.pause()
                                true
                            }
                            else -> false
                        }
                    }
                }.focusRequester(rootFocus)
                .focusable(),
    ) {
        PlayerSurface(
            player = player,
            modifier = Modifier.fillMaxSize().alpha(if (snapshot.pipeline.videoVisible) 1f else 0f),
        )

        if (controlsVisible && !menuOpen) {
            PlayerControlsPanel(
                item = item,
                snapshot = snapshot,
                callbacks =
                    PlayerActionCallbacks(
                        onExit = exit,
                        onTogglePlay = {
                            if (snapshot.status == PlaybackStatus.PAUSED) runtime.play() else runtime.pause()
                            controls.show()
                        },
                        onSeekBy = { seconds ->
                            runtime.seekBy(seconds)
                            controls.show()
                        },
                        onOpenAudioMenu = { menu = TrackMenuKind.AUDIO },
                        onOpenSubtitleMenu = { menu = TrackMenuKind.SUBTITLES },
                        onOpenEpisodes =
                            if (item.type == "Episode" && item.seriesId != null) {
                                {
                                    episodeMenuOpen = true
                                    scope.launch {
                                        seriesEpisodes =
                                            try {
                                                gateway.seriesEpisodes(userId, item.seriesId, item.id)
                                            } catch (_: Exception) {
                                                emptyList()
                                            }
                                    }
                                }
                            } else {
                                null
                            },
                        onPlayNext =
                            nextEpisode.view.nextEpisode?.let { next ->
                                {
                                    scope.launch {
                                        runtime.stop()
                                        onAdvance(next)
                                    }
                                }
                            },
                    ),
            )
        }

        val activeSkip = skip.activeSegment
        val overlayFree = !menuOpen && !snapshot.pipeline.visible
        val skipVisible = overlayFree && !nextEpisode.view.active
        if (activeSkip != null && skipVisible) {
            SkipSegmentButton(
                segment = activeSkip,
                onSkip = skip.skip,
                modifier = Modifier.align(Alignment.BottomEnd).padding(OVERLAY_MARGIN),
            )
        }

        if (nextEpisode.view.active && !menuOpen) {
            NextEpisodeCard(
                view = nextEpisode.view,
                onPlayNext = nextEpisode.playNext,
                onCancel = nextEpisode.cancel,
                modifier = Modifier.align(Alignment.BottomEnd).padding(OVERLAY_MARGIN),
            )
        }

        val openMenu = menu
        if (openMenu != null) {
            TrackMenuSheet(
                kind = openMenu,
                tracks = snapshot.tracks,
                onSelectAudio = { track ->
                    menu = null
                    controls.show()
                    scope.launch { runtime.selectAudioTrack(track) }
                },
                onSelectSubtitle = { track ->
                    menu = null
                    controls.show()
                    scope.launch { runtime.selectSubtitleTrack(track) }
                },
            )
        }

        if (episodeMenuOpen) {
            EpisodeMenuSheet(
                currentItemId = item.id,
                seriesName = item.seriesName,
                episodes = seriesEpisodes,
                onSelect = { episode ->
                    episodeMenuOpen = false
                    if (episode.id != item.id) {
                        scope.launch {
                            runtime.stop()
                            onAdvance(episode)
                        }
                    }
                },
            )
        }

        PipelineOverlay(
            itemName = if (item.type == "Episode") item.seriesName ?: item.name else item.name,
            backdropUrl = item.backdropUrl,
            progress = snapshot.pipeline,
        )
    }
}

private class SkipSegmentsState(
    val activeSegment: SkipSegment?,
    val skip: () -> Unit,
)

@Composable
private fun rememberSkipSegments(
    gateway: PlayerGateway,
    item: PlayableItem,
    snapshot: PlaybackSnapshot,
    runtime: PlaybackRuntime,
): SkipSegmentsState {
    var segments by remember { mutableStateOf<List<SkipSegment>>(emptyList()) }
    var dismissedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(item.id) {
        segments =
            try {
                normalizeSegments(gateway.mediaSegments(item.id))
            } catch (_: Exception) {
                emptyList()
            }
    }
    val positionTicks = (snapshot.positionSeconds * TICKS_PER_SECOND).toLong()
    val active = activeSegment(segments, positionTicks, dismissedIds)
    return SkipSegmentsState(
        activeSegment = active,
        skip = {
            if (active != null) {
                runtime.seekTo(active.endTicks.toDouble() / TICKS_PER_SECOND)
                dismissedIds = dismissedIds + active.id
            }
        },
    )
}

private class NextEpisodeUiState(
    val view: NextEpisodeView,
    val playNext: () -> Unit,
    val cancel: () -> Unit,
)

@Composable
private fun rememberNextEpisode(
    gateway: PlayerGateway,
    userId: String,
    item: PlayableItem,
    snapshot: PlaybackSnapshot,
    activeSegment: SkipSegment?,
    onAdvance: (PlayableItem) -> Unit,
    runtime: PlaybackRuntime,
    scope: CoroutineScope,
): NextEpisodeUiState {
    var next by remember { mutableStateOf<PlayableItem?>(null) }
    LaunchedEffect(item.id) {
        val seriesId = item.seriesId
        if (item.type != "Episode" || seriesId == null) return@LaunchedEffect
        next =
            try {
                selectFollowingEpisode(
                    gateway.followingEpisodes(userId, seriesId, item.id),
                    item.id,
                )
            } catch (_: Exception) {
                null
            }
    }
    val countdown =
        remember(next) {
            NextEpisodeCountdown(
                itemId = item.id,
                nextEpisode = next,
                onAdvance = { episode ->
                    scope.launch {
                        runtime.stop()
                        onAdvance(episode)
                    }
                },
            )
        }
    var view by
        remember(countdown) {
            mutableStateOf(NextEpisodeView(active = false, remainingSeconds = 0, nextEpisode = next))
        }
    val outroId = activeSegment?.takeIf { it.type == SegmentType.OUTRO }?.id
    val ended = snapshot.status == PlaybackStatus.ENDED
    LaunchedEffect(countdown, outroId, ended) {
        view = countdown.update(activeSegmentOutroId = outroId, ended = ended)
        while (view.active && view.remainingSeconds > 0) {
            delay(COUNTDOWN_TICK_MS)
            view = countdown.tick()
        }
    }
    return NextEpisodeUiState(
        view = view,
        playNext = { countdown.playNext() },
        cancel = {
            countdown.cancel()
            view = countdown.update(activeSegmentOutroId = outroId, ended = ended)
        },
    )
}

@Composable
private fun PlayerFailure(
    reason: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FAILURE_STACK_SPACING),
        modifier = modifier,
    ) {
        Text(
            text = reason,
            color = HomeflixColors.OnBackground,
            fontSize = LOADING_TEXT_SIZE.sp,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(FAILURE_BUTTON_SPACING),
        ) {
            PlayerFailureButton(label = "Retry", onClick = onRetry)
            PlayerFailureButton(label = "Back", onClick = onBack)
        }
    }
}

@Composable
private fun PlayerFailureButton(
    label: String,
    onClick: () -> Unit,
) {
    TvFocusSurface(
        contentDescription = label,
        onClick = onClick,
        appearance =
            TvFocusAppearance(
                shape = RoundedCornerShape(FAILURE_BUTTON_CORNER_RADIUS),
                backgroundColor = Color.White,
            ),
    ) {
        Text(
            text = label,
            color = FailureButtonText,
            fontSize = LOADING_TEXT_SIZE.sp,
            modifier =
                Modifier.padding(
                    horizontal = FAILURE_BUTTON_HORIZONTAL_PADDING,
                    vertical = FAILURE_BUTTON_VERTICAL_PADDING,
                ),
        )
    }
}
