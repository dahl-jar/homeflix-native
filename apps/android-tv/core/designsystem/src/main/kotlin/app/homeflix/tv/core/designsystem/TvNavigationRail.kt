package app.homeflix.tv.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.SubcomposeAsyncImage

data class TvNavProfile(
    val name: String,
    val avatarUrl: String?,
)

data class TvNavEntry(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
)

@Composable
fun TvNavigationRail(
    profile: TvNavProfile,
    entries: List<TvNavEntry>,
    contentFocusRequester: FocusRequester,
    onEntrySelected: (String) -> Unit,
    onProfileSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .width(RAIL_SCRIM_WIDTH)
                .background(
                    Brush.horizontalGradient(
                        0f to HomeflixColors.Background.copy(alpha = RAIL_SCRIM_ALPHA),
                        1f to Color.Transparent,
                    ),
                ),
    ) {
        RailTiles(
            profile = profile,
            entries = entries,
            contentFocusRequester = contentFocusRequester,
            onEntrySelected = onEntrySelected,
            onProfileSelected = onProfileSelected,
        )
    }
}

@Composable
private fun RailTiles(
    profile: TvNavProfile,
    entries: List<TvNavEntry>,
    contentFocusRequester: FocusRequester,
    onEntrySelected: (String) -> Unit,
    onProfileSelected: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
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
                    .size(TILE_SIZE)
                    .focusProperties {
                        right = contentFocusRequester
                    },
        ) {
            ProfileAvatar(profile)
            InactiveDim()
        }
        entries.forEach { entry ->
            TvFocusSurface(
                contentDescription = entryDescription(entry),
                onClick = { onEntrySelected(entry.id) },
                appearance = TvFocusAppearance(shape = CircleShape),
                modifier =
                    Modifier
                        .padding(top = ENTRY_SPACING)
                        .size(TILE_SIZE)
                        .focusProperties {
                            right = contentFocusRequester
                        },
            ) {
                Image(
                    imageVector = entry.icon,
                    contentDescription = null,
                    colorFilter =
                        ColorFilter.tint(
                            if (entry.selected) HomeflixColors.Focus else HomeflixColors.Muted,
                        ),
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .size(ENTRY_ICON_SIZE),
                )
            }
        }
    }
}

fun libraryNavIcon(collectionType: String?): ImageVector =
    when (collectionType) {
        "movies" -> Icons.Filled.Movie
        "tvshows" -> Icons.Filled.Tv
        else -> Icons.Filled.VideoLibrary
    }

private fun entryDescription(entry: TvNavEntry): String = if (entry.selected) "${entry.label} selected" else entry.label

@Composable
private fun InactiveDim() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(HomeflixColors.Background.copy(alpha = INACTIVE_DIM_ALPHA)),
    )
}

@Composable
private fun ProfileAvatar(profile: TvNavProfile) {
    if (profile.avatarUrl == null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(HomeflixColors.AvatarStart),
        ) {
            Text(
                text = profile.name.firstOrNull()?.uppercase() ?: "H",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        return
    }

    SubcomposeAsyncImage(
        model = profile.avatarUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        loading = { Box(Modifier.fillMaxSize().background(HomeflixColors.AvatarStart)) },
        error = { Box(Modifier.fillMaxSize().background(HomeflixColors.AvatarStart)) },
        modifier = Modifier.fillMaxSize(),
    )
}

private val NAV_RAIL_WIDTH = 44.dp
private val RAIL_SCRIM_WIDTH = 150.dp
private const val RAIL_SCRIM_ALPHA = 0.85f
private val TILE_SIZE = 34.dp
private val ENTRY_ICON_SIZE = 22.dp
private val ENTRY_SPACING = 22.dp
private const val INACTIVE_DIM_ALPHA = 0.45f
