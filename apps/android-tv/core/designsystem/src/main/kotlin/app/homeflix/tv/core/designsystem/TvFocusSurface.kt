package app.homeflix.tv.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun TvFocusSurface(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    appearance: TvFocusAppearance = TvFocusAppearance(),
    content: @Composable BoxScope.() -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by
        animateFloatAsState(
            targetValue = TvFocusStyle.scale(isFocused),
            animationSpec = tween(TvFocusStyle.FOCUS_MOTION_MILLIS, easing = FastOutSlowInEasing),
            label = "focusScale",
        )
    val borderColor =
        if (isFocused && appearance.showFocusBorder) HomeflixColors.Focus else appearance.unfocusedBorderColor
    val shape = appearance.shape

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.clip(shape)
                .background(appearance.backgroundColor)
                .border(HomeflixDimensions.FocusBorderWidth, borderColor, shape)
                .semantics { this.contentDescription = contentDescription }
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                }.clickable(onClick = onClick),
        content = content,
    )
}
