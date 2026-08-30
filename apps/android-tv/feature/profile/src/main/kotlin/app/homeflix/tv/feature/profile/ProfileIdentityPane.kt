package app.homeflix.tv.feature.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import app.homeflix.tv.core.designsystem.TvFocusAppearance
import app.homeflix.tv.core.designsystem.TvFocusSurface
import coil3.compose.SubcomposeAsyncImage

@Composable
internal fun IdentityPane(
    details: ProfileDetails,
    switchFocusRequester: FocusRequester,
    onSwitchProfile: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(IDENTITY_PANE_WIDTH),
    ) {
        ProfileAvatar(details)
        Spacer(Modifier.height(AVATAR_NAME_SPACING))
        Text(
            text = details.name,
            color = HomeflixColors.OnBackground,
            fontSize = NAME_FONT_SIZE,
            fontWeight = FontWeight.Normal,
        )
        Spacer(Modifier.height(NAME_SUBTITLE_SPACING))
        Text(
            text = "Homeflix profile",
            color = HomeflixColors.Muted,
            fontSize = SUBTITLE_FONT_SIZE,
        )
        Spacer(Modifier.height(SWITCH_TOP_SPACING))
        SwitchProfileButton(
            onSwitchProfile = onSwitchProfile,
            modifier = Modifier.focusRequester(switchFocusRequester),
        )
    }
}

@Composable
private fun SwitchProfileButton(
    onSwitchProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvFocusSurface(
        contentDescription = "Sign out",
        onClick = onSwitchProfile,
        appearance =
            TvFocusAppearance(
                shape = RoundedCornerShape(PILL_CORNER_PERCENT),
                backgroundColor = HomeflixColors.GlassBackground,
                unfocusedBorderColor = HomeflixColors.GlassBorder,
            ),
        modifier = modifier,
    ) {
        Text(
            text = "Switch profile",
            color = HomeflixColors.OnBackground,
            fontSize = PILL_FONT_SIZE,
            fontWeight = FontWeight.Medium,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = PILL_HORIZONTAL_PADDING, vertical = PILL_VERTICAL_PADDING),
        )
    }
}

@Composable
private fun ProfileAvatar(details: ProfileDetails) {
    val avatarModifier =
        Modifier
            .size(AVATAR_SIZE)
            .clip(RoundedCornerShape(AVATAR_CORNER_RADIUS))
    if (details.avatarUrl == null) {
        AvatarFallback(avatarModifier)
        return
    }

    SubcomposeAsyncImage(
        model = details.avatarUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        loading = { AvatarFallback(Modifier.fillMaxSize()) },
        error = { AvatarFallback(Modifier.fillMaxSize()) },
        modifier = avatarModifier,
    )
}

@Composable
private fun AvatarFallback(modifier: Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier.background(
                Brush.linearGradient(
                    colors = listOf(HomeflixColors.AvatarStart, HomeflixColors.AvatarEnd),
                ),
            ),
    ) {
        Image(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            colorFilter = ColorFilter.tint(HomeflixColors.OnBackground),
            modifier = Modifier.size(AVATAR_ICON_SIZE),
        )
    }
}

private const val PILL_CORNER_PERCENT = 50
internal val IDENTITY_PANE_WIDTH = 300.dp
private val AVATAR_SIZE = 110.dp
private val AVATAR_CORNER_RADIUS = 10.dp
private val AVATAR_ICON_SIZE = 44.dp
private val AVATAR_NAME_SPACING = 20.dp
private val NAME_SUBTITLE_SPACING = 6.dp
private val SWITCH_TOP_SPACING = 28.dp
private val PILL_HORIZONTAL_PADDING = 28.dp
private val PILL_VERTICAL_PADDING = 12.dp
private val NAME_FONT_SIZE = 32.sp
private val SUBTITLE_FONT_SIZE = 15.sp
private val PILL_FONT_SIZE = 16.sp
