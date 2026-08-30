package app.homeflix.tv.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors

private val COLUMN_WIDTH = 520.dp
private val HEADER_BOTTOM_SPACING = 28.dp
private val ROW_SPACING = 4.dp
private val ROW_CORNER_RADIUS = 6.dp
private val ROW_HORIZONTAL_PADDING = 20.dp
private val ROW_VERTICAL_PADDING = 12.dp
private val CHECK_SIZE = 22.dp
private val CHECK_SPACING = 14.dp
private val MENU_VERTICAL_PADDING = 120.dp
private const val HEADER_FONT_SIZE = 24
private const val ROW_FONT_SIZE = 18
private const val UNSELECTED_ROW_ALPHA = 0.85f
private val MenuBackground = Color(0xF7100E0F)
private val FocusedRowBackground = Color.White
private val FocusedRowContent = Color(0xFF141414)

enum class TrackMenuKind {
    AUDIO,
    SUBTITLES,
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TrackMenuSheet(
    kind: TrackMenuKind,
    tracks: TrackCatalog,
    onSelectAudio: (PlaybackTrack) -> Unit,
    onSelectSubtitle: (PlaybackTrack?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { initialFocus.requestFocus() } }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MenuBackground)
                .focusGroup()
                .focusProperties { exit = { FocusRequester.Cancel } },
    ) {
        TrackColumn(
            title = if (kind == TrackMenuKind.AUDIO) "Audio" else "Subtitles",
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .width(COLUMN_WIDTH)
                    .padding(vertical = MENU_VERTICAL_PADDING),
        ) {
            when (kind) {
                TrackMenuKind.AUDIO -> AudioRows(tracks, onSelectAudio, initialFocus)
                TrackMenuKind.SUBTITLES -> SubtitleRows(tracks, onSelectSubtitle, initialFocus)
            }
        }
    }
}

@Composable
private fun AudioRows(
    tracks: TrackCatalog,
    onSelect: (PlaybackTrack) -> Unit,
    initialFocus: FocusRequester,
) {
    val focusTargetTrack = tracks.selectedAudioTrack ?: tracks.audioTracks.firstOrNull()
    tracks.audioTracks.forEach { track ->
        TrackRow(
            label = track.label,
            selected = track == tracks.selectedAudioTrack,
            onClick = { onSelect(track) },
            modifier = if (track == focusTargetTrack) Modifier.focusRequester(initialFocus) else Modifier,
        )
    }
}

@Composable
private fun SubtitleRows(
    tracks: TrackCatalog,
    onSelect: (PlaybackTrack?) -> Unit,
    initialFocus: FocusRequester,
) {
    val offSelected = tracks.selectedSubtitleTrack == null
    TrackRow(
        label = "Off",
        selected = offSelected,
        onClick = { onSelect(null) },
        modifier = if (offSelected) Modifier.focusRequester(initialFocus) else Modifier,
    )
    tracks.subtitleTracks.forEach { track ->
        val selected = track == tracks.selectedSubtitleTrack
        TrackRow(
            label = track.label,
            selected = selected,
            onClick = { onSelect(track) },
            modifier = if (selected) Modifier.focusRequester(initialFocus) else Modifier,
        )
    }
}

@Composable
private fun TrackColumn(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Text(
            text = title,
            color = HomeflixColors.OnBackground,
            fontSize = HEADER_FONT_SIZE.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = HEADER_BOTTOM_SPACING, start = ROW_HORIZONTAL_PADDING),
        )
        Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
            content()
        }
    }
}

@Composable
private fun TrackRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .widthIn(min = COLUMN_WIDTH)
                .clip(RoundedCornerShape(ROW_CORNER_RADIUS))
                .background(if (focused) FocusedRowBackground else Color.Transparent)
                .semantics { contentDescription = label }
                .onFocusChanged { focused = it.isFocused }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ).padding(horizontal = ROW_HORIZONTAL_PADDING, vertical = ROW_VERTICAL_PADDING),
    ) {
        Box(modifier = Modifier.size(CHECK_SIZE)) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = if (focused) FocusedRowContent else HomeflixColors.OnBackground,
                    modifier = Modifier.size(CHECK_SIZE),
                )
            }
        }
        Text(
            text = label,
            color =
                when {
                    focused -> FocusedRowContent
                    selected -> HomeflixColors.OnBackground
                    else -> HomeflixColors.OnBackground.copy(alpha = UNSELECTED_ROW_ALPHA)
                },
            fontSize = ROW_FONT_SIZE.sp,
            fontWeight = if (selected || focused) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(start = CHECK_SPACING),
        )
    }
}
