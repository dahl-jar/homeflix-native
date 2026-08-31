package app.homeflix.tv.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

class TvFocusAppearance(
    val shape: Shape = RoundedCornerShape(HomeflixDimensions.CardCornerRadius),
    val backgroundColor: Color = Color.Transparent,
    val showFocusBorder: Boolean = true,
    val unfocusedBorderColor: Color = Color.Transparent,
)
