package app.homeflix.tv.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.tv.material3.*
import app.homeflix.tv.core.designsystem.HomeflixColors
import coil3.compose.AsyncImage

private val HEADER_HORIZONTAL_PADDING = 72.dp
private val HEADER_TOP_PADDING = 40.dp
private val HEADER_BOTTOM_PADDING = 20.dp
private val GRID_HORIZONTAL_PADDING = 60.dp
private val GRID_BOTTOM_PADDING = 40.dp
private val GRID_ITEM_SPACING = 8.dp
private val ROW_CORNER_RADIUS = 10.dp
private val ROW_PADDING = 8.dp
private val ROW_CONTENT_SPACING = 12.dp
private val STILL_WIDTH = 220.dp
private val STILL_HEIGHT = 124.dp
private val PLAY_BADGE_SIZE = 44.dp
private val PLAY_BADGE_ICON_SIZE = 24.dp
private val PROGRESS_TRACK_HEIGHT = 4.dp
private val FOCUS_BORDER_WIDTH = 2.dp
private val META_TOP_SPACING = 2.dp
private val OVERVIEW_TOP_SPACING = 6.dp
private const val EYEBROW_FONT_SIZE = 12
private const val SERIES_FONT_SIZE = 22
private const val TITLE_FONT_SIZE = 15
private const val RUNTIME_FONT_SIZE = 12
private const val OVERVIEW_FONT_SIZE = 12
private const val EYEBROW_LETTER_SPACING = 2.0
private const val GRID_COLUMNS = 2
private const val OVERVIEW_MAX_LINES = 3
private const val TICKS_PER_MINUTE = 600_000_000L
private const val MINUTES_PER_HOUR = 60
private val StillBackground = Color(0xFF211D1E)
private val PlayBadgeBackground = Color.Black.copy(alpha = 0.72f)
private val ProgressTrackColor = Color.White.copy(alpha = 0.35f)
private val FocusedRowFill = Color.White.copy(alpha = 0.1f)
private val CurrentRowFill = Color.White.copy(alpha = 0.06f)

@Composable
fun EpisodeMenuSheet(
    currentItemId: String,
    seriesName: String?,
    episodes: List<PlayableItem>,
    onSelect: (PlayableItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(episodes.isNotEmpty()) {
        if (episodes.isNotEmpty()) runCatching { initialFocus.requestFocus() }
    }
    Column(
        modifier =
            modifier.playerMenuSurface(),
    ) {
        EpisodeMenuHeader(seriesName)
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            horizontalArrangement = Arrangement.spacedBy(GRID_ITEM_SPACING),
            verticalArrangement = Arrangement.spacedBy(GRID_ITEM_SPACING),
            contentPadding =
                PaddingValues(
                    start = GRID_HORIZONTAL_PADDING,
                    end = GRID_HORIZONTAL_PADDING,
                    bottom = GRID_BOTTOM_PADDING,
                ),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(episodes, key = PlayableItem::id) { episode ->
                val current = episode.id == currentItemId
                EpisodeCard(
                    episode = episode,
                    current = current,
                    onClick = { onSelect(episode) },
                    modifier = if (current) Modifier.focusRequester(initialFocus) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun EpisodeMenuHeader(seriesName: String?) {
    Column(
        modifier =
            Modifier
                .padding(horizontal = HEADER_HORIZONTAL_PADDING)
                .padding(top = HEADER_TOP_PADDING, bottom = HEADER_BOTTOM_PADDING),
    ) {
        Text(
            text = "EPISODES",
            color = HomeflixColors.Muted,
            fontSize = EYEBROW_FONT_SIZE.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = EYEBROW_LETTER_SPACING.sp,
        )
        if (seriesName != null) {
            Text(
                text = seriesName,
                color = HomeflixColors.OnBackground,
                fontSize = SERIES_FONT_SIZE.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: PlayableItem,
    current: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusState = rememberPlayerFocusState()
    val focused = focusState.isFocused
    val label = episodeLabel(episode)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_CONTENT_SPACING),
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ROW_CORNER_RADIUS))
                .background(
                    when {
                        focused -> FocusedRowFill
                        current -> CurrentRowFill
                        else -> Color.Transparent
                    },
                ).border(
                    width = FOCUS_BORDER_WIDTH,
                    color = if (focused) HomeflixColors.Focus else Color.Transparent,
                    shape = RoundedCornerShape(ROW_CORNER_RADIUS),
                ).playerFocusableClick(
                    state = focusState,
                    contentDescription = "Play $label",
                    onClick = onClick,
                ).padding(ROW_PADDING),
    ) {
        EpisodeStill(episode = episode, current = current)
        EpisodeMeta(episode = episode, label = label, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun EpisodeMeta(
    episode: PlayableItem,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = HomeflixColors.OnBackground,
            fontSize = TITLE_FONT_SIZE.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val runtime = episodeRuntimeText(episode.runTimeTicks)
        if (runtime != null) {
            Text(
                text = runtime,
                color = HomeflixColors.Muted,
                fontSize = RUNTIME_FONT_SIZE.sp,
                modifier = Modifier.padding(top = META_TOP_SPACING),
            )
        }
        val overview = episode.overview
        if (overview != null) {
            Text(
                text = overview,
                color = HomeflixColors.Muted,
                fontSize = OVERVIEW_FONT_SIZE.sp,
                maxLines = OVERVIEW_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = OVERVIEW_TOP_SPACING),
            )
        }
    }
}

@Composable
private fun EpisodeStill(
    episode: PlayableItem,
    current: Boolean,
) {
    Box(
        modifier =
            Modifier
                .width(STILL_WIDTH)
                .height(STILL_HEIGHT)
                .clip(RoundedCornerShape(ROW_CORNER_RADIUS))
                .background(StillBackground),
    ) {
        if (episode.primaryImageUrl != null) {
            AsyncImage(
                model = episode.primaryImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (current) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .size(PLAY_BADGE_SIZE)
                        .clip(CircleShape)
                        .background(PlayBadgeBackground),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = HomeflixColors.OnBackground,
                    modifier = Modifier.size(PLAY_BADGE_ICON_SIZE),
                )
            }
        }
        val progress = episodeProgress(episode)
        if (progress > 0f) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(PROGRESS_TRACK_HEIGHT)
                        .background(ProgressTrackColor),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(fraction = progress)
                            .height(PROGRESS_TRACK_HEIGHT)
                            .background(HomeflixColors.Focus),
                )
            }
        }
    }
}

private fun episodeLabel(episode: PlayableItem): String {
    val episodeCode =
        listOfNotNull(
            episode.parentIndexNumber?.let { "S$it" },
            episode.indexNumber?.let { "E$it" },
        ).joinToString("")
    return listOf(episodeCode, episode.name).filter(String::isNotEmpty).joinToString(" · ")
}

internal fun episodeRuntimeText(runTimeTicks: Long?): String? {
    if (runTimeTicks == null || runTimeTicks <= 0) return null
    val totalMinutes = runTimeTicks / TICKS_PER_MINUTE
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

internal fun episodeProgress(episode: PlayableItem): Float {
    val runTimeTicks = episode.runTimeTicks
    return if (runTimeTicks == null || runTimeTicks <= 0) {
        0f
    } else {
        (episode.resumePositionTicks.toFloat() / runTimeTicks).coerceIn(0f, 1f)
    }
}
