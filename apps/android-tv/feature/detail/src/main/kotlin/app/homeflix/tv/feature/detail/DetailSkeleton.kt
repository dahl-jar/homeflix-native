package app.homeflix.tv.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.HomeflixDimensions

@Composable
internal fun DetailSkeleton() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(HomeflixColors.Background)
                .padding(start = CONTENT_START_PADDING, top = SKELETON_TOP_PADDING)
                .semantics { contentDescription = "Loading details" },
    ) {
        SkeletonBlock(width = TITLE_WIDTH, height = TITLE_HEIGHT)
        Spacer(Modifier.height(TITLE_CHIP_SPACING))
        SkeletonBlock(width = CHIP_ROW_WIDTH, height = CHIP_ROW_HEIGHT)
        Spacer(Modifier.height(CHIP_OVERVIEW_SPACING))
        SkeletonBlock(width = OVERVIEW_WIDTH, height = OVERVIEW_HEIGHT)
        Spacer(Modifier.height(OVERVIEW_PILL_SPACING))
        SkeletonBlock(width = PILL_WIDTH, height = PILL_HEIGHT)
        Spacer(Modifier.height(PILL_ROW_SPACING))
        Row(horizontalArrangement = Arrangement.spacedBy(CARD_SPACING)) {
            repeat(CARD_COUNT) {
                SkeletonBlock(width = CARD_WIDTH, height = CARD_HEIGHT)
            }
        }
    }
}

@Composable
private fun SkeletonBlock(
    width: Dp,
    height: Dp,
) {
    Spacer(
        modifier =
            Modifier
                .size(width, height)
                .clip(RoundedCornerShape(HomeflixDimensions.CardCornerRadius))
                .background(HomeflixColors.Surface),
    )
}

private const val CARD_COUNT = 5
private val CONTENT_START_PADDING = 72.dp
private val SKELETON_TOP_PADDING = 180.dp
private val TITLE_WIDTH = 420.dp
private val TITLE_HEIGHT = 40.dp
private val TITLE_CHIP_SPACING = 16.dp
private val CHIP_ROW_WIDTH = 260.dp
private val CHIP_ROW_HEIGHT = 20.dp
private val CHIP_OVERVIEW_SPACING = 18.dp
private val OVERVIEW_WIDTH = 640.dp
private val OVERVIEW_HEIGHT = 54.dp
private val OVERVIEW_PILL_SPACING = 22.dp
private val PILL_WIDTH = 140.dp
private val PILL_HEIGHT = 44.dp
private val PILL_ROW_SPACING = 32.dp
private val CARD_SPACING = 14.dp
private val CARD_WIDTH = 180.dp
private val CARD_HEIGHT = 101.dp
