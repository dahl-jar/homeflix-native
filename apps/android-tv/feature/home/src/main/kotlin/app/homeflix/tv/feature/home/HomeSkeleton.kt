package app.homeflix.tv.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.HomeflixDimensions

@Composable
internal fun HomeSkeleton() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(HomeflixColors.Background)
                .semantics { contentDescription = "Loading Homeflix" },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(HERO_BLOCK_HEIGHT)
                    .background(HomeflixColors.Surface),
        )
        Column(
            modifier = Modifier.padding(start = HOME_HORIZONTAL_PADDING, top = RAIL_START),
        ) {
            SkeletonBlock(width = LABEL_WIDTH, height = LABEL_HEIGHT, cornerRadius = LABEL_CORNER_RADIUS)
            Spacer(Modifier.height(LABEL_CARD_SPACING))
            Row(horizontalArrangement = Arrangement.spacedBy(CARD_SPACING)) {
                repeat(CARD_COUNT) {
                    SkeletonBlock(
                        width = CARD_WIDTH,
                        height = CARD_HEIGHT,
                        cornerRadius = HomeflixDimensions.CardCornerRadius,
                    )
                }
            }
        }
        HomeHeader(modifier = Modifier.align(Alignment.TopStart))
    }
}

@Composable
private fun SkeletonBlock(
    width: Dp,
    height: Dp,
    cornerRadius: Dp,
) {
    Box(
        modifier =
            Modifier
                .size(width, height)
                .clip(RoundedCornerShape(cornerRadius))
                .background(HomeflixColors.Surface),
    )
}

private const val CARD_COUNT = 7
private val HERO_BLOCK_HEIGHT = 260.dp
private val LABEL_WIDTH = 160.dp
private val LABEL_HEIGHT = 16.dp
private val LABEL_CORNER_RADIUS = 4.dp
private val LABEL_CARD_SPACING = 12.dp
private val CARD_SPACING = 14.dp
private val CARD_WIDTH = 108.dp
private val CARD_HEIGHT = 162.dp
