package app.homeflix.tv.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvFocusSurface
import coil3.compose.SubcomposeAsyncImage

@Composable
internal fun DetailPosterRail(
    title: String,
    items: List<MediaItem>,
    onMediaSelected: (String) -> Unit,
) {
    Column {
        RailTitle(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = FOCUS_OVERFLOW_GUTTER, vertical = FOCUS_OVERFLOW_GUTTER),
            horizontalArrangement = Arrangement.spacedBy(CARD_SPACING),
            modifier = Modifier.railFocusOutset(FOCUS_OVERFLOW_GUTTER),
        ) {
            items(
                items = items,
                key = MediaItem::id,
            ) { item ->
                PosterCard(
                    item = item,
                    onClick = { onMediaSelected(item.id) },
                )
            }
        }
    }
}

private fun Modifier.railFocusOutset(gutter: androidx.compose.ui.unit.Dp): Modifier =
    layout { measurable, constraints ->
        val extra = (gutter * 2).roundToPx()
        val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra))
        layout(constraints.maxWidth, placeable.height) {
            placeable.place(-extra / 2, 0)
        }
    }

@Composable
internal fun DetailCastRow(cast: List<CastMember>) {
    Column {
        RailTitle("Cast")
        Spacer(Modifier.height(TITLE_ROW_SPACING))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(CAST_SPACING)) {
            items(
                items = cast,
                key = CastMember::id,
            ) { person ->
                CastCard(person)
            }
        }
    }
}

@Composable
private fun RailTitle(title: String) {
    Text(
        text = title,
        color = HomeflixColors.OnBackground,
        fontSize = RAIL_TITLE_FONT_SIZE,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun PosterCard(
    item: MediaItem,
    onClick: () -> Unit,
) {
    TvFocusSurface(
        contentDescription = "${item.name} card",
        onClick = onClick,
        modifier = Modifier.size(POSTER_WIDTH, POSTER_HEIGHT),
    ) {
        DetailRailImage(item.primaryImageUrl ?: item.backdropImageUrl)
    }
}

@Composable
private fun CastCard(person: CastMember) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(CAST_CARD_WIDTH),
    ) {
        Box(
            modifier =
                Modifier
                    .size(CAST_IMAGE_SIZE)
                    .clip(CircleShape),
        ) {
            DetailRailImage(person.imageUrl)
        }
        Spacer(Modifier.height(CAST_NAME_SPACING))
        Text(
            text = person.name,
            color = HomeflixColors.Muted,
            fontSize = CAST_NAME_FONT_SIZE,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DetailRailImage(imageUrl: String?) {
    if (imageUrl == null) {
        RailImageFallback()
        return
    }
    SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        loading = { RailImageFallback() },
        error = { RailImageFallback() },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun RailImageFallback() {
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

private val TITLE_ROW_SPACING = 10.dp
private val FOCUS_OVERFLOW_GUTTER = 12.dp
private val CARD_SPACING = 14.dp
private val POSTER_WIDTH = 108.dp
private val POSTER_HEIGHT = 162.dp
private val CAST_SPACING = 18.dp
private val CAST_CARD_WIDTH = 84.dp
private val CAST_IMAGE_SIZE = 84.dp
private val CAST_NAME_SPACING = 6.dp
private val RAIL_TITLE_FONT_SIZE = 17.sp
private val CAST_NAME_FONT_SIZE = 12.sp
