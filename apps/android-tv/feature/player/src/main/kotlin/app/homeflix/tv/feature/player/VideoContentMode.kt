package app.homeflix.tv.feature.player

import androidx.compose.ui.layout.ContentScale

enum class VideoContentMode(
    val contentScale: ContentScale,
    val actionLabel: String,
) {
    FIT(ContentScale.Fit, "Fill screen"),
    FILL(ContentScale.Crop, "Fit video"),
    ;

    fun next(): VideoContentMode =
        when (this) {
            FIT -> FILL
            FILL -> FIT
        }
}
