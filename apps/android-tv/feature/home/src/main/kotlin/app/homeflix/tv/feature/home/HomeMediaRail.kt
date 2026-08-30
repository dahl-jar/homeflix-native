package app.homeflix.tv.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvFocusSurface
import coil3.compose.SubcomposeAsyncImage
import kotlin.math.roundToInt

private val FEATURED_CARD_WIDTH = 168.dp
private val FEATURED_CARD_HEIGHT = 94.dp
private val LANDSCAPE_CARD_WIDTH = 180.dp
private val LANDSCAPE_CARD_HEIGHT = 101.dp
private val POSTER_CARD_WIDTH = 108.dp
private val POSTER_CARD_HEIGHT = 162.dp
private const val CARD_GRADIENT_STOP = 0.48f
private const val PERCENT_MAX = 100f
private val RAIL_EDGE_FADE = 18.dp

internal data class HomeRailFocus(
    val firstCard: FocusRequester,
    val entry: FocusRequester,
)

@Composable
internal fun HomeMediaRail(
    rail: HomeRail,
    railFocus: HomeRailFocus?,
    onFocused: (HomeMediaItem) -> Unit,
    onMediaSelected: (String) -> Unit,
    featured: Boolean = false,
) {
    Column {
        Text(
            text = rail.title,
            color = HomeflixColors.OnBackground,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = HOME_HORIZONTAL_PADDING),
        )
        Spacer(Modifier.height(10.dp))
        HomeHorizontalFocusPositioning(startOffset = HOME_HORIZONTAL_PADDING - HOME_NAV_GUTTER) {
            LazyRow(
                contentPadding =
                    PaddingValues(
                        start = HOME_HORIZONTAL_PADDING - HOME_NAV_GUTTER,
                        end = HOME_HORIZONTAL_PADDING,
                    ),
                horizontalArrangement =
                    androidx.compose.foundation.layout.Arrangement
                        .spacedBy(14.dp),
                modifier =
                    Modifier
                        .padding(start = HOME_NAV_GUTTER)
                        .railEntry(railFocus)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush =
                                    Brush.horizontalGradient(
                                        0f to HomeflixColors.Background,
                                        1f to Color.Transparent,
                                        endX = RAIL_EDGE_FADE.toPx(),
                                    ),
                                size = Size(RAIL_EDGE_FADE.toPx(), size.height),
                            )
                        },
            ) {
                itemsIndexed(
                    items = rail.items,
                    key = { _, item -> item.id },
                ) { index, item ->
                    MediaCard(
                        item = item,
                        dimensions = cardDimensions(rail.variant, featured),
                        onFocused = { onFocused(item) },
                        onClick = { onMediaSelected(HomePolicy.selectionId(item)) },
                        modifier =
                            if (index == 0 && railFocus != null) {
                                Modifier.focusRequester(railFocus.firstCard)
                            } else {
                                Modifier
                            },
                    )
                }
            }
        }
    }
}

private fun Modifier.railEntry(railFocus: HomeRailFocus?): Modifier =
    if (railFocus != null) {
        focusRequester(railFocus.entry).focusRestorer()
    } else {
        this
    }

@Composable
private fun MediaCard(
    item: HomeMediaItem,
    dimensions: CardDimensions,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    TvFocusSurface(
        contentDescription = "${item.name} card",
        onClick = onClick,
        modifier =
            modifier
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) onFocused()
                }.size(dimensions.width, dimensions.height)
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
                            1f to Color.Black.copy(alpha = 0.8f),
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
            ProgressBar(
                percentage = percentage,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun CardImage(item: HomeMediaItem) {
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
private fun ProgressBar(
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

private data class CardDimensions(
    val width: Dp,
    val height: Dp,
)

private fun cardDimensions(
    variant: HomeRailVariant,
    featured: Boolean,
): CardDimensions =
    when {
        featured -> CardDimensions(FEATURED_CARD_WIDTH, FEATURED_CARD_HEIGHT)
        variant == HomeRailVariant.Landscape -> CardDimensions(LANDSCAPE_CARD_WIDTH, LANDSCAPE_CARD_HEIGHT)
        else -> CardDimensions(POSTER_CARD_WIDTH, POSTER_CARD_HEIGHT)
    }
