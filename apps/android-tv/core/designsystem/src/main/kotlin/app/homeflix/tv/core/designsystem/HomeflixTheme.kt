package app.homeflix.tv.core.designsystem

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val HomeflixColorScheme =
    darkColorScheme(
        primary = HomeflixColors.Focus,
        background = HomeflixColors.Background,
        surface = HomeflixColors.Surface,
        onPrimary = HomeflixColors.Background,
        onBackground = HomeflixColors.OnBackground,
        onSurface = HomeflixColors.OnBackground,
    )

@Composable
fun HomeflixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HomeflixColorScheme,
        content = content,
    )
}
