package app.homeflix.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush

private const val GRADIENT_MIDDLE_STOP = 0.35f
private const val GRADIENT_BACKGROUND_STOP = 0.7f

@Composable
fun HomeflixScreenBackground(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops =
                            arrayOf(
                                0f to HomeflixColors.BackgroundGradientStart,
                                GRADIENT_MIDDLE_STOP to HomeflixColors.BackgroundGradientMiddle,
                                GRADIENT_BACKGROUND_STOP to HomeflixColors.Background,
                            ),
                    ),
                ),
    )
}
