package app.homeflix.tv.feature.home

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.HomeflixDimensions

@Composable
internal fun HomeHeader(modifier: Modifier = Modifier) {
    Text(
        text = "HOMEFLIX",
        color = HomeflixColors.Focus,
        fontSize = 18.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 3.sp,
        modifier = modifier.padding(HomeflixDimensions.WordmarkEdgePadding),
    )
}
