package app.homeflix.tv.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Text
import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvFocusSurface
import coil3.compose.SubcomposeAsyncImage
import kotlin.math.roundToInt

private const val GRID_COLUMNS = 7
private const val POSTER_ASPECT_RATIO = 2f / 3f
private const val CARD_GRADIENT_STOP = 0.48f
private const val CARD_SCRIM_ALPHA = 0.8f
private const val PERCENT_MAX = 100f
private val CARD_SPACING = 14.dp

@Composable
internal fun LibraryGrid(
    items: List<MediaItem>,
    gridState: LazyGridState,
    contentPadding: PaddingValues,
    onMediaSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(CARD_SPACING),
        verticalArrangement = Arrangement.spacedBy(CARD_SPACING),
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        items(
            items = items,
            key = MediaItem::id,
        ) { item ->
            LibraryCard(
                item = item,
                onClick = { onMediaSelected(item.id) },
            )
        }
    }
}

@Composable
private fun LibraryCard(
    item: MediaItem,
    onClick: () -> Unit,
) {
    TvFocusSurface(
        contentDescription = "${item.name} card",
        onClick = onClick,
        modifier =
            Modifier
                .aspectRatio(POSTER_ASPECT_RATIO)
                .zIndex(1f),
    ) {
        CardImage(item)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            CARD_GRADIENT_STOP to Color.Transparent,
                            1f to Color.Black.copy(alpha = CARD_SCRIM_ALPHA),
                        ),
                    ),
        )
        Text(
            text = item.name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 9.dp, vertical = 8.dp),
        )
        item.playedPercentage?.let { percentage ->
            CardProgress(
                percentage = percentage,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun CardImage(item: MediaItem) {
    val imageUrl = item.primaryImageUrl ?: item.backdropImageUrl
    if (imageUrl == null) {
        CardFallback()
    } else {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            loading = { CardFallback() },
            error = { CardFallback() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun CardFallback() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(HomeflixColors.AvatarStart, HomeflixColors.AvatarEnd),
                    ),
                ),
    )
}

@Composable
private fun CardProgress(
    percentage: Float,
    modifier: Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(3.dp)
                .semantics { contentDescription = "${percentage.roundToInt()}% watched" }
                .background(Color.White.copy(alpha = 0.32f)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((percentage / PERCENT_MAX).coerceIn(0f, 1f))
                    .background(HomeflixColors.Focus),
        )
    }
}
