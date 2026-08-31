package app.homeflix.tv.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.HomeflixDimensions
import app.homeflix.tv.core.designsystem.TvFocusAppearance
import app.homeflix.tv.core.designsystem.TvFocusSurface

@Composable
internal fun LibraryHeader(
    name: String,
    total: Int?,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier) {
        Text(
            text = name,
            color = HomeflixColors.OnBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        if (total != null) {
            Text(
                text = total.toString(),
                color = HomeflixColors.Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = COUNT_SPACING, bottom = COUNT_BASELINE_PADDING),
            )
        }
    }
}

@Composable
internal fun LibrarySkeleton(
    libraryName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(SKELETON_SPACING),
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Loading $libraryName" },
    ) {
        repeat(SKELETON_ROWS) {
            Row(horizontalArrangement = Arrangement.spacedBy(SKELETON_SPACING)) {
                repeat(SKELETON_COLUMNS) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .aspectRatio(SKELETON_POSTER_ASPECT_RATIO)
                                .clip(RoundedCornerShape(HomeflixDimensions.CardCornerRadius))
                                .background(HomeflixColors.Surface),
                    )
                }
            }
        }
    }
}

@Composable
internal fun LibraryError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = STATUS_TOP_PADDING)) {
        Text(
            text = "Can’t load this library.",
            color = HomeflixColors.OnBackground,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(RETRY_SPACING))
        TvFocusSurface(
            contentDescription = "Retry",
            onClick = onRetry,
            appearance =
                TvFocusAppearance(
                    shape = RoundedCornerShape(RETRY_CORNER_RADIUS),
                    backgroundColor = HomeflixColors.GlassBackground,
                ),
        ) {
            Text(
                text = "Retry",
                color = HomeflixColors.OnBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = RETRY_HORIZONTAL_PADDING, vertical = RETRY_VERTICAL_PADDING),
            )
        }
    }
}

private const val SKELETON_ROWS = 2
private const val SKELETON_COLUMNS = 7
private const val SKELETON_POSTER_ASPECT_RATIO = 2f / 3f
private val SKELETON_SPACING = 14.dp
private val COUNT_SPACING = 10.dp
private val COUNT_BASELINE_PADDING = 5.dp
private val STATUS_TOP_PADDING = 24.dp
private val RETRY_SPACING = 12.dp
private val RETRY_CORNER_RADIUS = 17.dp
private val RETRY_HORIZONTAL_PADDING = 16.dp
private val RETRY_VERTICAL_PADDING = 7.dp
