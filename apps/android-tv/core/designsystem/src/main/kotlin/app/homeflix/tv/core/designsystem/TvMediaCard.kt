package app.homeflix.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage
import kotlin.math.roundToInt

@Composable
fun TvMediaCard(
    name: String,
    imageUrl: String?,
    playedPercentage: Float?,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvFocusSurface(
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
    ) {
        TvCardImage(imageUrl)
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
            text = name,
            color = Color.White,
            fontSize = CARD_NAME_FONT_SIZE,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = CARD_NAME_HORIZONTAL_PADDING, vertical = CARD_NAME_VERTICAL_PADDING),
        )
        playedPercentage?.let { percentage ->
            TvWatchedProgress(
                percentage = percentage,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
fun TvCardImage(imageUrl: String?) {
    if (imageUrl == null) {
        TvCardImageFallback()
    } else {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            loading = { TvCardImageFallback() },
            error = { TvCardImageFallback() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun TvCardImageFallback() {
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
private fun TvWatchedProgress(
    percentage: Float,
    modifier: Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(PROGRESS_TRACK_HEIGHT)
                .semantics { contentDescription = "${percentage.roundToInt()}% watched" }
                .background(Color.White.copy(alpha = PROGRESS_TRACK_ALPHA)),
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

private const val CARD_GRADIENT_STOP = 0.48f
private const val CARD_SCRIM_ALPHA = 0.8f
private const val PERCENT_MAX = 100f
private const val PROGRESS_TRACK_ALPHA = 0.32f
private val PROGRESS_TRACK_HEIGHT = 3.dp
private val CARD_NAME_FONT_SIZE = 11.sp
private val CARD_NAME_HORIZONTAL_PADDING = 9.dp
private val CARD_NAME_VERTICAL_PADDING = 8.dp
