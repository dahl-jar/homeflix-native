package app.homeflix.tv.feature.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvFocusStyle

private val TOP_BAR_PADDING = 24.dp
private val TOP_BAR_SPACING = 8.dp
private val ICON_BUTTON_SIZE = 54.dp
private val ICON_SIZE = 30.dp
private val PROMINENT_BUTTON_SIZE = 70.dp
private val PROMINENT_ICON_SIZE = 38.dp
private val SEEK_ICON_SIZE = 44.dp
private val CENTER_CLUSTER_SPACING = 120.dp
private val BOTTOM_PADDING = 24.dp
private val BOTTOM_HORIZONTAL_PADDING = 40.dp
private val TIMELINE_HEIGHT = 4.dp
private val TIMELINE_TEXT_SPACING = 8.dp
private val ACTION_BUTTON_WIDTH = 104.dp
private val ACTION_LABEL_SPACING = 5.dp
private val ACTION_ICON_SIZE = 24.dp
private val ACTION_ROW_TOP_SPACING = 4.dp
private val ACTION_BUTTON_VERTICAL_PADDING = 8.dp
private const val TITLE_FONT_SIZE = 17
private const val TIME_FONT_SIZE = 13
private const val ACTION_FONT_SIZE = 11
private const val FOCUS_MOTION_MILLIS = 240
private const val UNFOCUSED_ALPHA = 0.8f
private val ACTION_ICON_CIRCLE_SIZE = 40.dp
private val FocusedControlBackground = Color.White
private val FocusedControlContent = Color(0xFF141414)
private const val SCRIM_ALPHA = 0.34f
private val TimelineTrack = Color.White.copy(alpha = 0.24f)
private val TimelineBuffer = Color.White.copy(alpha = 0.38f)

data class PlayerActionCallbacks(
    val onExit: () -> Unit,
    val onTogglePlay: () -> Unit,
    val onSeekBy: (Double) -> Unit,
    val onOpenAudioMenu: () -> Unit,
    val onOpenSubtitleMenu: () -> Unit,
    val onOpenEpisodes: (() -> Unit)?,
    val onPlayNext: (() -> Unit)?,
)

@Composable
fun PlayerControlsPanel(
    item: PlayableItem,
    snapshot: PlaybackSnapshot,
    callbacks: PlayerActionCallbacks,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black.copy(alpha = SCRIM_ALPHA))) {
        TopBar(
            item = item,
            onExit = callbacks.onExit,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        CenterCluster(
            snapshot = snapshot,
            callbacks = callbacks,
            modifier = Modifier.align(Alignment.Center),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = BOTTOM_HORIZONTAL_PADDING)
                    .padding(bottom = BOTTOM_PADDING),
        ) {
            Timeline(snapshot)
            ActionRow(
                item = item,
                callbacks = callbacks,
                modifier = Modifier.padding(top = ACTION_ROW_TOP_SPACING),
            )
        }
    }
}

@Composable
private fun TopBar(
    item: PlayableItem,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TOP_BAR_SPACING),
        modifier = modifier.fillMaxWidth().padding(TOP_BAR_PADDING),
    ) {
        PlayerIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBackIos,
            contentDescription = "Close player",
            onClick = onExit,
        )
        Text(
            text = item.name,
            color = HomeflixColors.OnBackground,
            fontSize = TITLE_FONT_SIZE.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CenterCluster(
    snapshot: PlaybackSnapshot,
    callbacks: PlayerActionCallbacks,
    modifier: Modifier = Modifier,
) {
    val playing = snapshot.status == PlaybackStatus.PLAYING
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { playFocus.requestFocus() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CENTER_CLUSTER_SPACING),
        modifier = modifier,
    ) {
        PlayerIconButton(
            icon = Icons.Filled.Replay10,
            contentDescription = "Rewind 10 seconds",
            onClick = { callbacks.onSeekBy(-PLAYER_SEEK_STEP_SECONDS) },
            iconSize = SEEK_ICON_SIZE,
        )
        PlayerIconButton(
            icon = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (playing) "Pause" else "Play",
            onClick = callbacks.onTogglePlay,
            prominent = true,
            modifier = Modifier.focusRequester(playFocus),
        )
        PlayerIconButton(
            icon = Icons.Filled.Forward10,
            contentDescription = "Forward 10 seconds",
            onClick = { callbacks.onSeekBy(PLAYER_SEEK_STEP_SECONDS) },
            iconSize = SEEK_ICON_SIZE,
        )
    }
}

@Composable
private fun Timeline(
    snapshot: PlaybackSnapshot,
    modifier: Modifier = Modifier,
) {
    val duration = snapshot.durationSeconds
    val progress = if (duration > 0) (snapshot.positionSeconds / duration).toFloat().coerceIn(0f, 1f) else 0f
    val buffered = if (duration > 0) (snapshot.bufferedSeconds / duration).toFloat().coerceIn(0f, 1f) else 0f
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(TIMELINE_HEIGHT)
                    .clip(CircleShape)
                    .background(TimelineTrack),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(fraction = buffered)
                        .height(TIMELINE_HEIGHT)
                        .background(TimelineBuffer),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(fraction = progress)
                        .height(TIMELINE_HEIGHT)
                        .background(HomeflixColors.Focus),
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(top = TIMELINE_TEXT_SPACING),
        ) {
            Text(
                text = formatPlaybackTime(snapshot.positionSeconds),
                color = HomeflixColors.OnBackground,
                fontSize = TIME_FONT_SIZE.sp,
            )
            Text(
                text = formatPlaybackTime(snapshot.durationSeconds),
                color = HomeflixColors.Muted,
                fontSize = TIME_FONT_SIZE.sp,
            )
        }
    }
}

@Composable
private fun ActionRow(
    item: PlayableItem,
    callbacks: PlayerActionCallbacks,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier.fillMaxWidth(),
    ) {
        PlayerActionButton(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            label = "Audio",
            onClick = callbacks.onOpenAudioMenu,
        )
        PlayerActionButton(
            icon = Icons.Filled.Subtitles,
            label = "Subtitles",
            onClick = callbacks.onOpenSubtitleMenu,
        )
        val onOpenEpisodes = callbacks.onOpenEpisodes
        if (item.type == "Episode" && onOpenEpisodes != null) {
            PlayerActionButton(
                icon = Icons.Filled.VideoLibrary,
                label = "Episodes",
                onClick = onOpenEpisodes,
            )
        }
        val onPlayNext = callbacks.onPlayNext
        if (onPlayNext != null) {
            PlayerActionButton(
                icon = Icons.Filled.SkipNext,
                label = "Next Episode",
                onClick = onPlayNext,
            )
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    iconSize: Dp = ICON_SIZE,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by
        animateFloatAsState(
            targetValue = TvFocusStyle.scale(focused),
            animationSpec = tween(FOCUS_MOTION_MILLIS, easing = FastOutSlowInEasing),
            label = "playerIconFocus",
        )
    val background = if (focused) FocusedControlBackground else Color.Transparent
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(if (prominent) PROMINENT_BUTTON_SIZE else ICON_BUTTON_SIZE)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = if (focused) 1f else UNFOCUSED_ALPHA
                }.clip(CircleShape)
                .background(background)
                .semantics { this.contentDescription = contentDescription }
                .onFocusChanged { focused = it.isFocused }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (focused) FocusedControlContent else HomeflixColors.OnBackground,
            modifier = Modifier.size(if (prominent) PROMINENT_ICON_SIZE else iconSize),
        )
    }
}

@Composable
private fun PlayerActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by
        animateFloatAsState(
            targetValue = TvFocusStyle.scale(focused),
            animationSpec = tween(FOCUS_MOTION_MILLIS, easing = FastOutSlowInEasing),
            label = "playerActionFocus",
        )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .width(ACTION_BUTTON_WIDTH)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = if (focused) 1f else UNFOCUSED_ALPHA
                }.semantics { this.contentDescription = label }
                .onFocusChanged { focused = it.isFocused }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ).padding(vertical = ACTION_BUTTON_VERTICAL_PADDING),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(ACTION_ICON_CIRCLE_SIZE)
                    .clip(CircleShape)
                    .background(if (focused) FocusedControlBackground else Color.Transparent),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (focused) FocusedControlContent else HomeflixColors.OnBackground,
                modifier = Modifier.size(ACTION_ICON_SIZE),
            )
        }
        Text(
            text = label,
            color = HomeflixColors.OnBackground,
            fontSize = ACTION_FONT_SIZE.sp,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = ACTION_LABEL_SPACING),
        )
    }
}
