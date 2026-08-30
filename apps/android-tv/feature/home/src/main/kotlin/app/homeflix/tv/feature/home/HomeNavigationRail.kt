package app.homeflix.tv.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvFocusAppearance
import app.homeflix.tv.core.designsystem.TvFocusSurface
import coil3.compose.SubcomposeAsyncImage

@Composable
internal fun HomeNavigationRail(
    viewer: HomeViewer,
    contentFocusRequester: FocusRequester,
    onProfileSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .padding(start = 20.dp, top = 68.dp)
                .width(NAV_RAIL_WIDTH),
    ) {
        TvFocusSurface(
            contentDescription = "Switch profile",
            onClick = onProfileSelected,
            appearance =
                TvFocusAppearance(
                    shape = CircleShape,
                    backgroundColor = HomeflixColors.GlassBackground,
                ),
            modifier =
                Modifier
                    .size(34.dp)
                    .focusProperties {
                        right = contentFocusRequester
                        down = contentFocusRequester
                    },
        ) {
            ViewerAvatar(viewer)
            InactiveDim()
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .padding(top = 22.dp)
                    .size(ACTIVE_TILE_SIZE)
                    .semantics { contentDescription = "Home selected" },
        ) {
            Image(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                colorFilter = ColorFilter.tint(HomeflixColors.Focus),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun InactiveDim() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(HomeflixColors.Background.copy(alpha = INACTIVE_DIM_ALPHA)),
    )
}

private val NAV_RAIL_WIDTH = 44.dp
private val ACTIVE_TILE_SIZE = 34.dp
private const val INACTIVE_DIM_ALPHA = 0.45f

@Composable
private fun ViewerAvatar(viewer: HomeViewer) {
    if (viewer.avatarUrl == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(HomeflixColors.AvatarStart),
        ) {
            Text(
                text = viewer.name.firstOrNull()?.uppercase() ?: "H",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        return
    }

    SubcomposeAsyncImage(
        model = viewer.avatarUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        loading = { Box(Modifier.fillMaxSize().background(HomeflixColors.AvatarStart)) },
        error = { Box(Modifier.fillMaxSize().background(HomeflixColors.AvatarStart)) },
        modifier = Modifier.fillMaxSize(),
    )
}
