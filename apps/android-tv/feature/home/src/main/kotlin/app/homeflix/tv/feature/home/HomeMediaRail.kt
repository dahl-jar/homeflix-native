package app.homeflix.tv.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Text
import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvMediaCard

private val FEATURED_CARD_WIDTH = 168.dp
private val FEATURED_CARD_HEIGHT = 94.dp
private val LANDSCAPE_CARD_WIDTH = 180.dp
private val LANDSCAPE_CARD_HEIGHT = 101.dp
private val POSTER_CARD_WIDTH = 108.dp
private val POSTER_CARD_HEIGHT = 162.dp

internal data class HomeRailFocus(
    val firstCard: FocusRequester,
    val entry: FocusRequester,
)

@Composable
internal fun HomeMediaRail(
    rail: HomeRail,
    railFocus: HomeRailFocus?,
    onFocused: (MediaItem) -> Unit,
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
                        .focusRestorer(),
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

@Composable
private fun MediaCard(
    item: MediaItem,
    dimensions: CardDimensions,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    TvMediaCard(
        name = item.name,
        imageUrl = item.primaryImageUrl ?: item.backdropImageUrl,
        playedPercentage = item.playedPercentage,
        contentDescription = "${item.name} card",
        onClick = onClick,
        modifier =
            modifier
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) onFocused()
                }.size(dimensions.width, dimensions.height)
                .zIndex(1f),
    )
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
