package app.homeflix.tv.feature.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvFocusAppearance
import app.homeflix.tv.core.designsystem.TvFocusSurface

@Composable
internal fun DetailSummary(
    content: DetailContent,
    playFocusRequester: FocusRequester,
    onPlaySelected: (String) -> Unit,
    onRestartSelected: (String) -> Unit,
    onPlayFocused: () -> Unit,
) {
    val item = content.item
    Column {
        Text(
            text = item.name,
            color = HomeflixColors.OnBackground,
            fontSize = TITLE_FONT_SIZE,
            fontWeight = FontWeight.Bold,
            maxLines = TITLE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = TEXT_MAX_WIDTH),
        )
        Spacer(Modifier.height(TITLE_CHIP_SPACING))
        Row(horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING)) {
            detailChips(item).forEach { label ->
                DetailChip(label = label)
            }
            starText(item)?.let { star ->
                DetailChip(label = star, starred = true)
            }
        }
        item.overview?.let { overview ->
            Spacer(Modifier.height(CHIP_OVERVIEW_SPACING))
            Text(
                text = overview,
                color = HomeflixColors.Muted,
                fontSize = OVERVIEW_FONT_SIZE,
                lineHeight = OVERVIEW_LINE_HEIGHT,
                maxLines = OVERVIEW_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = TEXT_MAX_WIDTH),
            )
        }
        Spacer(Modifier.height(PLAY_TOP_SPACING))
        DetailActionsRow(
            item = item,
            playFocusRequester = playFocusRequester,
            onPlaySelected = onPlaySelected,
            onRestartSelected = onRestartSelected,
            onPlayFocused = onPlayFocused,
        )
    }
}

@Composable
private fun DetailActionsRow(
    item: MediaItem,
    playFocusRequester: FocusRequester,
    onPlaySelected: (String) -> Unit,
    onRestartSelected: (String) -> Unit,
    onPlayFocused: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ACTION_SPACING),
    ) {
        PlayButton(
            label = playLabel(item),
            contentDescription = "Play ${item.name}",
            onClick = { onPlaySelected(item.id) },
            modifier =
                Modifier
                    .focusRequester(playFocusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onPlayFocused()
                        }
                    },
        )
        ActionButton(label = "Trailer", icon = Icons.Filled.Theaters)
        ActionButton(label = "Mark Played", icon = Icons.Filled.CheckCircle)
        ActionButton(
            label = "Restart",
            icon = Icons.Filled.Refresh,
            onClick = { onRestartSelected(item.id) },
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit = {},
) {
    TvFocusSurface(
        contentDescription = label,
        onClick = onClick,
        appearance = TvFocusAppearance(shape = RoundedCornerShape(BUTTON_CORNER_RADIUS)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = ACTION_HORIZONTAL_PADDING, vertical = ACTION_VERTICAL_PADDING),
        ) {
            Image(
                imageVector = icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(HomeflixColors.Muted),
                modifier = Modifier.size(ACTION_ICON_SIZE),
            )
            Spacer(Modifier.height(ACTION_ICON_SPACING))
            Text(
                text = label,
                color = Color.White,
                fontSize = ACTION_FONT_SIZE,
            )
        }
    }
}

@Composable
private fun DetailChip(
    label: String,
    starred: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .clip(RoundedCornerShape(CHIP_CORNER_PERCENT))
                .background(ChipBackground)
                .border(CHIP_BORDER_WIDTH, ChipBorder, RoundedCornerShape(CHIP_CORNER_PERCENT))
                .padding(horizontal = CHIP_HORIZONTAL_PADDING, vertical = CHIP_VERTICAL_PADDING),
    ) {
        if (starred) {
            Image(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                colorFilter = ColorFilter.tint(HomeflixColors.Rating),
                modifier = Modifier.size(STAR_SIZE).padding(end = STAR_SPACING),
            )
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = CHIP_FONT_SIZE,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PlayButton(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvFocusSurface(
        contentDescription = contentDescription,
        onClick = onClick,
        appearance =
            TvFocusAppearance(
                shape = RoundedCornerShape(BUTTON_CORNER_RADIUS),
                backgroundColor = Color.White,
            ),
        modifier = modifier,
    ) {
        Text(
            text = "▶ $label",
            color = ButtonText,
            fontSize = BUTTON_FONT_SIZE,
            fontWeight = FontWeight.SemiBold,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = BUTTON_HORIZONTAL_PADDING, vertical = BUTTON_VERTICAL_PADDING),
        )
    }
}

@Composable
internal fun DetailRetryButton(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvFocusSurface(
        contentDescription = "Retry",
        onClick = onRetry,
        appearance =
            TvFocusAppearance(
                shape = RoundedCornerShape(BUTTON_CORNER_RADIUS),
                backgroundColor = Color.White,
            ),
        modifier = modifier,
    ) {
        Text(
            text = "Retry",
            color = ButtonText,
            fontSize = BUTTON_FONT_SIZE,
            fontWeight = FontWeight.SemiBold,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = BUTTON_HORIZONTAL_PADDING, vertical = BUTTON_VERTICAL_PADDING),
        )
    }
}

private const val TITLE_MAX_LINES = 2
private const val OVERVIEW_MAX_LINES = 3
private const val CHIP_CORNER_PERCENT = 50
private const val CHIP_BACKGROUND_ALPHA = 0.08f
private const val CHIP_BORDER_ALPHA = 0.12f
private val ChipBackground = Color.White.copy(alpha = CHIP_BACKGROUND_ALPHA)
private val ChipBorder = Color.White.copy(alpha = CHIP_BORDER_ALPHA)
private val ButtonText = Color(0xFF141414)
private val TEXT_MAX_WIDTH = 640.dp
private val TITLE_CHIP_SPACING = 14.dp
private val CHIP_SPACING = 8.dp
private val CHIP_OVERVIEW_SPACING = 16.dp
private val PLAY_TOP_SPACING = 22.dp
private val CHIP_BORDER_WIDTH = 1.dp
private val CHIP_HORIZONTAL_PADDING = 14.dp
private val CHIP_VERTICAL_PADDING = 6.dp
private val STAR_SIZE = 18.dp
private val STAR_SPACING = 4.dp
private val BUTTON_CORNER_RADIUS = 8.dp
private val BUTTON_HORIZONTAL_PADDING = 36.dp
private val BUTTON_VERTICAL_PADDING = 13.dp
private val ACTION_SPACING = 10.dp
private val ACTION_HORIZONTAL_PADDING = 14.dp
private val ACTION_VERTICAL_PADDING = 6.dp
private val ACTION_ICON_SIZE = 20.dp
private val ACTION_ICON_SPACING = 4.dp
private val ACTION_FONT_SIZE = 11.sp
private val TITLE_FONT_SIZE = 40.sp
private val CHIP_FONT_SIZE = 13.sp
private val OVERVIEW_FONT_SIZE = 15.sp
private val OVERVIEW_LINE_HEIGHT = 22.sp
private val BUTTON_FONT_SIZE = 16.sp
