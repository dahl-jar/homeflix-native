package app.homeflix.tv.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvFocusAppearance
import app.homeflix.tv.core.designsystem.TvFocusSurface

private val CARD_CORNER_RADIUS = 10.dp
private val CARD_PADDING = 18.dp
private val CARD_SPACING = 10.dp
private val CARD_MAX_WIDTH = 340.dp
private val BUTTON_CORNER_RADIUS = 8.dp
private val BUTTON_HORIZONTAL_PADDING = 16.dp
private val BUTTON_VERTICAL_PADDING = 8.dp
private const val CARD_TITLE_FONT_SIZE = 15
private const val CARD_BODY_FONT_SIZE = 13
private const val BUTTON_FONT_SIZE = 14
private val CardBackground = Color(0xF0141213)
private val ButtonBackground = Color.White.copy(alpha = 0.1f)
private val ButtonTextDark = Color(0xFF141414)

@Composable
fun SkipSegmentButton(
    segment: SkipSegment,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label =
        when (segment.type) {
            SegmentType.INTRO -> "Skip Intro"
            SegmentType.RECAP -> "Skip Recap"
            SegmentType.OUTRO -> "Skip Outro"
        }
    OverlayPillButton(label = label, filled = true, onClick = onSkip, modifier = modifier)
}

@Composable
fun NextEpisodeCard(
    view: NextEpisodeView,
    onPlayNext: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val next = view.nextEpisode ?: return
    Column(
        verticalArrangement = Arrangement.spacedBy(CARD_SPACING),
        modifier =
            modifier
                .widthIn(max = CARD_MAX_WIDTH)
                .clip(RoundedCornerShape(CARD_CORNER_RADIUS))
                .background(CardBackground)
                .padding(CARD_PADDING),
    ) {
        Text(
            text = "Up next",
            color = HomeflixColors.Muted,
            fontSize = CARD_BODY_FONT_SIZE.sp,
            fontWeight = FontWeight.Medium,
        )
        val episodeCode =
            listOfNotNull(
                next.parentIndexNumber?.let { "S$it" },
                next.indexNumber?.let { "E$it" },
            ).joinToString("")
        Text(
            text = listOf(episodeCode, next.name).filter(String::isNotEmpty).joinToString(" · "),
            color = HomeflixColors.OnBackground,
            fontSize = CARD_TITLE_FONT_SIZE.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Playing in ${view.remainingSeconds}s",
            color = HomeflixColors.Muted,
            fontSize = CARD_BODY_FONT_SIZE.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(CARD_SPACING)) {
            OverlayPillButton(label = "Play now", filled = true, onClick = onPlayNext)
            OverlayPillButton(label = "Cancel", filled = false, onClick = onCancel)
        }
    }
}

@Composable
private fun OverlayPillButton(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvFocusSurface(
        contentDescription = label,
        onClick = onClick,
        appearance =
            TvFocusAppearance(
                shape = RoundedCornerShape(BUTTON_CORNER_RADIUS),
                backgroundColor = if (filled) Color.White else ButtonBackground,
                showFocusBorder = !filled,
            ),
        modifier = modifier,
    ) {
        Text(
            text = label,
            color = if (filled) ButtonTextDark else HomeflixColors.OnBackground,
            fontSize = BUTTON_FONT_SIZE.sp,
            fontWeight = FontWeight.SemiBold,
            modifier =
                Modifier
                    .padding(horizontal = BUTTON_HORIZONTAL_PADDING, vertical = BUTTON_VERTICAL_PADDING),
        )
    }
}
