package app.homeflix.tv.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.HomeflixDimensions
import app.homeflix.tv.core.designsystem.HomeflixScreenBackground
import app.homeflix.tv.core.designsystem.HomeflixWordmark
import app.homeflix.tv.core.designsystem.TvFocusSurface
import coil3.compose.SubcomposeAsyncImage

private val AVATAR_SIZE = 110.dp
private val PROFILE_NAME_SPACING = 8.dp
private val TITLE_TOP_PADDING = 140.dp
private val TITLE_BOTTOM_SPACING = 32.dp

@Composable
fun ProfileSelectionScreen(
    profiles: List<AuthProfile>,
    onProfileSelected: (AuthProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstProfileFocusRequester = remember { FocusRequester() }

    LaunchedEffect(profiles) {
        if (profiles.isNotEmpty()) {
            firstProfileFocusRequester.requestFocus()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HomeflixScreenBackground()
        HomeflixWordmark(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(HomeflixDimensions.WordmarkEdgePadding),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = TITLE_TOP_PADDING),
        ) {
            Text(
                text = "Who’s watching?",
                color = HomeflixColors.OnBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
            )
            Spacer(Modifier.height(TITLE_BOTTOM_SPACING))
            ProfileRail(
                profiles = profiles,
                firstProfileFocusRequester = firstProfileFocusRequester,
                onProfileSelected = onProfileSelected,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProfileRail(
    profiles: List<AuthProfile>,
    firstProfileFocusRequester: FocusRequester,
    onProfileSelected: (AuthProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.height(HomeflixDimensions.ProfileCardHeight),
    ) {
        val cardWidth = HomeflixDimensions.ProfileCardWidth * profiles.size.toFloat()
        val gapCount = (profiles.size - 1).coerceAtLeast(0)
        val gapWidth = HomeflixDimensions.ContentSpacing * gapCount.toFloat()
        val railPadding = ((maxWidth - cardWidth - gapWidth) / 2).coerceAtLeast(0.dp)

        LazyRow(
            contentPadding = PaddingValues(horizontal = railPadding),
            horizontalArrangement = Arrangement.spacedBy(HomeflixDimensions.ContentSpacing),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                items = profiles,
                key = { _, profile -> profile.id },
            ) { index, profile ->
                ProfileCard(
                    profile = profile,
                    onClick = { onProfileSelected(profile) },
                    modifier =
                        if (index == 0) {
                            Modifier.focusRequester(firstProfileFocusRequester)
                        } else {
                            Modifier
                        },
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: AuthProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.size(HomeflixDimensions.ProfileCardWidth, HomeflixDimensions.ProfileCardHeight),
    ) {
        TvFocusSurface(
            contentDescription = "${profile.name} profile",
            onClick = onClick,
            modifier = modifier.size(AVATAR_SIZE),
        ) {
            ProfileAvatar(profile = profile)
        }
        Spacer(Modifier.height(PROFILE_NAME_SPACING))
        Text(
            text = profile.name,
            color = HomeflixColors.Muted,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun ProfileAvatar(profile: AuthProfile) {
    if (profile.avatarUrl == null) {
        AvatarFallback()
        return
    }
    SubcomposeAsyncImage(
        model = profile.avatarUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        loading = { AvatarFallback() },
        error = { AvatarFallback() },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun AvatarFallback() {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(HomeflixColors.AvatarStart, HomeflixColors.AvatarEnd),
                    ),
                ),
    ) {
        Image(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            colorFilter = ColorFilter.tint(HomeflixColors.OnBackground),
            modifier = Modifier.size(44.dp),
        )
    }
}
