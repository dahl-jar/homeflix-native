package app.homeflix.tv.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvFocusSurface
import coil3.compose.SubcomposeAsyncImage

@Composable
internal fun DetailSeasonsRow(
    seasons: List<DetailSeason>,
    seasonIndex: Int,
    episodes: List<MediaItem>,
    onSeasonSelected: (Int) -> Unit,
    onEpisodeSelected: (String) -> Unit,
) {
    Column {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(TAB_SPACING)) {
            itemsIndexed(
                items = seasons,
                key = { _, season -> season.id },
            ) { index, season ->
                SeasonTab(
                    name = season.name,
                    selected = index == seasonIndex,
                    onClick = { onSeasonSelected(index) },
                )
            }
        }
        Spacer(Modifier.height(TAB_EPISODE_SPACING))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(EPISODE_SPACING)) {
            items(
                items = episodes,
                key = MediaItem::id,
            ) { episode ->
                EpisodeCard(
                    episode = episode,
                    onClick = { onEpisodeSelected(episode.id) },
                )
            }
        }
    }
}

@Composable
private fun SeasonTab(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .width(IntrinsicSize.Max)
                .semantics { contentDescription = if (selected) "$name selected" else name }
                .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                .clickable(onClick = onClick),
    ) {
        Text(
            text = name,
            color = if (selected || isFocused) Color.White else HomeflixColors.Muted,
            fontSize = TAB_FONT_SIZE,
            fontWeight = FontWeight.Medium,
            modifier =
                Modifier.padding(horizontal = TAB_HORIZONTAL_PADDING, vertical = TAB_VERTICAL_PADDING),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(TAB_UNDERLINE_HEIGHT)
                    .background(
                        when {
                            isFocused -> HomeflixColors.Focus
                            selected -> Color.White
                            else -> Color.Transparent
                        },
                    ),
        )
    }
}

@Composable
private fun EpisodeCard(
    episode: MediaItem,
    onClick: () -> Unit,
) {
    val titleText =
        episode.indexNumber?.let { index -> "$index. ${episode.name}" } ?: episode.name
    Column(modifier = Modifier.width(EPISODE_WIDTH)) {
        TvFocusSurface(
            contentDescription = "$titleText card",
            onClick = onClick,
            modifier = Modifier.size(EPISODE_WIDTH, EPISODE_HEIGHT),
        ) {
            EpisodeThumb(episode)
        }
        Spacer(Modifier.height(EPISODE_TITLE_SPACING))
        Text(
            text = titleText,
            color = HomeflixColors.OnBackground,
            fontSize = EPISODE_TITLE_FONT_SIZE,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        episode.runTimeTicks?.let { ticks ->
            Text(
                text = runtimeText(ticks),
                color = HomeflixColors.Muted,
                fontSize = EPISODE_RUNTIME_FONT_SIZE,
            )
        }
    }
}

@Composable
private fun EpisodeThumb(episode: MediaItem) {
    val imageUrl = episode.primaryImageUrl ?: episode.backdropImageUrl
    if (imageUrl == null) {
        ThumbFallback()
        return
    }
    SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        loading = { ThumbFallback() },
        error = { ThumbFallback() },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun ThumbFallback() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(HomeflixColors.AvatarStart, HomeflixColors.AvatarEnd),
                    ),
                ),
    )
}

private val TAB_SPACING = 14.dp
private val TAB_EPISODE_SPACING = 16.dp
private val TAB_HORIZONTAL_PADDING = 10.dp
private val TAB_VERTICAL_PADDING = 6.dp
private val TAB_UNDERLINE_HEIGHT = 2.dp
private val EPISODE_SPACING = 14.dp
private val EPISODE_WIDTH = 180.dp
private val EPISODE_HEIGHT = 101.dp
private val EPISODE_TITLE_SPACING = 8.dp
private val TAB_FONT_SIZE = 13.sp
private val EPISODE_TITLE_FONT_SIZE = 13.sp
private val EPISODE_RUNTIME_FONT_SIZE = 12.sp
