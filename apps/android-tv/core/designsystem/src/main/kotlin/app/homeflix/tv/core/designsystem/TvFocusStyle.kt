package app.homeflix.tv.core.designsystem

object TvFocusStyle {
    const val FOCUS_MOTION_MILLIS = 240

    private const val RESTING_SCALE = 1.0f
    private const val FOCUSED_SCALE = 1.12f

    fun scale(isFocused: Boolean): Float = if (isFocused) FOCUSED_SCALE else RESTING_SCALE
}
