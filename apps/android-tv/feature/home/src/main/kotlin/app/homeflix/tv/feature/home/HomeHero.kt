package app.homeflix.tv.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import coil3.compose.SubcomposeAsyncImage
import java.time.ZonedDateTime

private const val BACKDROP_CROSSFADE_MILLIS = 320
private const val OVERVIEW_LINES = 2
private const val HORIZONTAL_SCRIM_MID = 0.32f
private const val HORIZONTAL_SCRIM_MID_ALPHA = 0.66f
private const val HORIZONTAL_SCRIM_STOP = 0.62f
private const val VERTICAL_SCRIM_STOP = 0.66f
private const val VERTICAL_SCRIM_MID_ALPHA = 0.36f
private const val OVERVIEW_ALPHA = 0.84f
private const val METADATA_ALPHA = 0.92f
private val HERO_HEIGHT = 380.dp
private val HERO_CONTENT_TOP = 88.dp
private val HERO_DETAILS_WIDTH = 430.dp
private val RATING_STAR_SIZE = 14.dp

@Composable
internal fun HomeHero(item: HomeMediaItem?) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(HERO_HEIGHT),
    ) {
        HeroBackdrop(imageUrl = item?.backdropImageUrl)
        if (item != null) {
            HeroDetails(
                item = item,
                modifier =
                    Modifier.padding(
                        start = HOME_HORIZONTAL_PADDING,
                        top = HERO_CONTENT_TOP,
                    ),
            )
        }
    }
}

@Composable
private fun HeroBackdrop(imageUrl: String?) {
    Box(modifier = Modifier.fillMaxSize()) {
        HeroArtwork(
            imageUrl = imageUrl,
            modifier = Modifier.fillMaxSize(),
        )
        HeroScrims()
    }
}

@Composable
private fun HeroArtwork(
    imageUrl: String?,
    modifier: Modifier,
) {
    Box(
        modifier =
            modifier.semantics { contentDescription = "Hero artwork" },
    ) {
        Crossfade(
            targetState = imageUrl,
            animationSpec = tween(BACKDROP_CROSSFADE_MILLIS),
            label = "heroBackdrop",
        ) { target ->
            if (target == null) {
                BackdropFallback()
            } else {
                SubcomposeAsyncImage(
                    model = target,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    loading = { BackdropFallback() },
                    error = { BackdropFallback() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun HeroScrims() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to HomeflixColors.Background,
                        HORIZONTAL_SCRIM_MID to HomeflixColors.Background.copy(alpha = HORIZONTAL_SCRIM_MID_ALPHA),
                        HORIZONTAL_SCRIM_STOP to Color.Transparent,
                    ),
                ),
    )
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        VERTICAL_SCRIM_STOP to HomeflixColors.Background.copy(alpha = VERTICAL_SCRIM_MID_ALPHA),
                        1f to HomeflixColors.Background,
                    ),
                ),
    )
}

@Composable
private fun HeroDetails(
    item: HomeMediaItem,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .width(HERO_DETAILS_WIDTH)
                .semantics { contentDescription = "Hero ${item.name}" },
    ) {
        Text(
            text = item.seriesName ?: item.name,
            color = HomeflixColors.OnBackground,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        HeroMetadataLine(item)
        item.overview?.takeIf(String::isNotBlank)?.let { overview ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = overview,
                color = HomeflixColors.OnBackground.copy(alpha = OVERVIEW_ALPHA),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = OVERVIEW_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HeroMetadataLine(item: HomeMediaItem) {
    val now = remember(item.id) { ZonedDateTime.now() }
    val segments = HomeHeroMetadata.segments(item, now)
    if (segments.isEmpty()) return
    val ratingLabel = HomeHeroMetadata.communityRatingLabel(item)

    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = "Hero metadata" },
    ) {
        segments.forEachIndexed { index, segment ->
            if (index > 0) {
                MetadataSeparator()
            }
            MetadataText(segment)
            if (segment == ratingLabel) {
                Image(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(HomeflixColors.Rating),
                    modifier = Modifier.size(RATING_STAR_SIZE),
                )
            }
        }
    }
}

@Composable
private fun MetadataSeparator() {
    Text(
        text = "•",
        color = HomeflixColors.Muted,
        fontSize = 13.sp,
    )
}

@Composable
private fun MetadataText(value: String) {
    Text(
        text = value,
        color = HomeflixColors.OnBackground.copy(alpha = METADATA_ALPHA),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun BackdropFallback() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(HomeflixColors.BackgroundGradientStart, HomeflixColors.Background),
                    ),
                ),
    )
}
