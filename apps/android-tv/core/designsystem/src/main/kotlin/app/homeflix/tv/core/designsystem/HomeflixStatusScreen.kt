package app.homeflix.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable
fun HomeflixWordmark(modifier: Modifier = Modifier) {
    Text(
        text = "HOMEFLIX",
        color = HomeflixColors.Focus,
        fontSize = WORDMARK_FONT_SIZE,
        fontWeight = FontWeight.Black,
        letterSpacing = WORDMARK_LETTER_SPACING,
        modifier = modifier,
    )
}

@Composable
fun HomeflixStatusScreen(
    title: String,
    detail: String?,
    modifier: Modifier = Modifier,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .fillMaxSize()
                .background(HomeflixColors.Background),
    ) {
        HomeflixWordmark(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(HomeflixDimensions.WordmarkEdgePadding),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = HomeflixColors.OnBackground,
                fontSize = STATUS_TITLE_FONT_SIZE,
                textAlign = TextAlign.Center,
            )
            detail?.let {
                Spacer(Modifier.height(STATUS_DETAIL_SPACING))
                Text(
                    text = it,
                    color = HomeflixColors.Muted,
                    fontSize = STATUS_DETAIL_FONT_SIZE,
                    textAlign = TextAlign.Center,
                )
            }
            actions?.invoke(this)
        }
    }
}

private val WORDMARK_FONT_SIZE = 18.sp
private val WORDMARK_LETTER_SPACING = 3.sp
private val STATUS_TITLE_FONT_SIZE = 24.sp
private val STATUS_DETAIL_SPACING = 12.dp
private val STATUS_DETAIL_FONT_SIZE = 16.sp
