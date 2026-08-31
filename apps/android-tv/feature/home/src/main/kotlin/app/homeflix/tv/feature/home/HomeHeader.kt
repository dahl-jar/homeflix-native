package app.homeflix.tv.feature.home

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.homeflix.tv.core.designsystem.HomeflixDimensions
import app.homeflix.tv.core.designsystem.HomeflixWordmark

@Composable
internal fun HomeHeader(modifier: Modifier = Modifier) {
    HomeflixWordmark(
        modifier = modifier.padding(HomeflixDimensions.WordmarkEdgePadding),
    )
}
