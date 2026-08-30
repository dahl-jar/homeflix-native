package app.homeflix.tv.feature.home

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.homeflix.tv.core.designsystem.TvFocusStyle

private fun focusScrollSpec(): AnimationSpec<Float> =
    tween(
        TvFocusStyle.FOCUS_MOTION_MILLIS,
        easing = FastOutSlowInEasing,
    )

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeVerticalFocusPositioning(content: @Composable () -> Unit) {
    val titleAllowancePx = with(LocalDensity.current) { RAIL_TITLE_ALLOWANCE.toPx() }
    val focusPositioning =
        remember(titleAllowancePx) {
            object : BringIntoViewSpec {
                override val scrollAnimationSpec: AnimationSpec<Float> = focusScrollSpec()

                override fun calculateScrollDistance(
                    offset: Float,
                    size: Float,
                    containerSize: Float,
                ): Float = offset - titleAllowancePx
            }
        }

    CompositionLocalProvider(LocalBringIntoViewSpec provides focusPositioning, content = content)
}

private val RAIL_TITLE_ALLOWANCE = 40.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeHorizontalFocusPositioning(
    startOffset: Dp,
    content: @Composable () -> Unit,
) {
    val startOffsetPx = with(LocalDensity.current) { startOffset.toPx() }
    val focusPositioning =
        remember(startOffsetPx) {
            object : BringIntoViewSpec {
                override val scrollAnimationSpec: AnimationSpec<Float> = focusScrollSpec()

                override fun calculateScrollDistance(
                    offset: Float,
                    size: Float,
                    containerSize: Float,
                ): Float = offset - startOffsetPx
            }
        }

    CompositionLocalProvider(LocalBringIntoViewSpec provides focusPositioning, content = content)
}
