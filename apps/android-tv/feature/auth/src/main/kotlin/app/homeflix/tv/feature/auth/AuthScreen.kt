package app.homeflix.tv.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.HomeflixStatusScreen
import app.homeflix.tv.core.designsystem.TvFocusAppearance
import app.homeflix.tv.core.designsystem.TvFocusSurface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private sealed interface AuthContentState {
    data object Loading : AuthContentState

    data object Unavailable : AuthContentState

    data class Profiles(
        val profiles: List<AuthProfile>,
    ) : AuthContentState
}

private sealed interface AuthRoute {
    data object Profiles : AuthRoute

    data class Pin(
        val profile: AuthProfile,
    ) : AuthRoute
}

@Composable
fun AuthScreen(
    gateway: AuthGateway,
    onAuthenticated: (AuthenticatedUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    var contentState by remember(gateway) { mutableStateOf<AuthContentState>(AuthContentState.Loading) }
    var route by remember(gateway) { mutableStateOf<AuthRoute>(AuthRoute.Profiles) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(gateway) {
        contentState =
            try {
                AuthContentState.Profiles(gateway.fetchPublicProfiles())
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                AuthContentState.Unavailable
            }
    }

    when (val state = contentState) {
        AuthContentState.Loading -> AuthLoadingScreen(modifier)
        AuthContentState.Unavailable -> AuthUnavailableScreen(modifier = modifier)
        is AuthContentState.Profiles ->
            when (val currentRoute = route) {
                AuthRoute.Profiles ->
                    ProfileSelectionScreen(
                        profiles = state.profiles,
                        onProfileSelected = { profile ->
                            when (val action = ProfileSelection.select(profile)) {
                                is ProfileSelectionAction.RequestPin -> route = AuthRoute.Pin(action.profile)
                                is ProfileSelectionAction.Authenticate ->
                                    scope.launch {
                                        authenticate(gateway, action.profile, action.pin)?.let(onAuthenticated)
                                    }
                            }
                        },
                        modifier = modifier,
                    )

                is AuthRoute.Pin ->
                    PinScreen(
                        onBack = { route = AuthRoute.Profiles },
                        onPinSubmitted = { pin ->
                            authenticate(gateway, currentRoute.profile, pin)?.let { authenticated ->
                                onAuthenticated(authenticated)
                                true
                            } ?: false
                        },
                        modifier = modifier,
                    )
            }
    }
}

@Composable
fun AuthLoadingScreen(modifier: Modifier = Modifier) {
    HomeflixStatusScreen(
        title = "Loading profiles…",
        detail = null,
        modifier = modifier,
    )
}

@Composable
fun AuthUnavailableScreen(
    onRetry: (() -> Unit)? = null,
    onChangeServer: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    HomeflixStatusScreen(
        title = "Can’t reach the server.",
        detail = "Check the server configuration and network",
        modifier = modifier,
    ) {
        if (onRetry != null || onChangeServer != null) {
            Spacer(Modifier.height(STATUS_ACTION_SPACING))
            Row(horizontalArrangement = Arrangement.spacedBy(STATUS_ACTION_GAP)) {
                onRetry?.let { retry ->
                    StatusActionButton(label = "Retry", primary = true, onClick = retry)
                }
                onChangeServer?.let { changeServer ->
                    StatusActionButton(label = "Change server", primary = false, onClick = changeServer)
                }
            }
        }
    }
}

@Composable
private fun StatusActionButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    TvFocusSurface(
        contentDescription = label,
        onClick = onClick,
        appearance =
            TvFocusAppearance(
                shape = StatusActionShape,
                backgroundColor = if (primary) Color.White else HomeflixColors.GlassBackground,
            ),
        modifier = Modifier.size(width = STATUS_ACTION_WIDTH, height = STATUS_ACTION_HEIGHT),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = label,
                color = if (primary) PrimaryActionTextColor else HomeflixColors.OnBackground,
                fontSize = STATUS_ACTION_FONT_SIZE,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private val PrimaryActionTextColor = Color(0xFF141414)
private val StatusActionShape = RoundedCornerShape(8.dp)
private val STATUS_ACTION_SPACING = 28.dp
private val STATUS_ACTION_GAP = 16.dp
private val STATUS_ACTION_WIDTH = 170.dp
private val STATUS_ACTION_HEIGHT = 44.dp
private val STATUS_ACTION_FONT_SIZE = 15.sp

private suspend fun authenticate(
    gateway: AuthGateway,
    profile: AuthProfile,
    password: String,
): AuthenticatedUser? =
    try {
        gateway.authenticate(profile.name, password)
    } catch (failure: CancellationException) {
        throw failure
    } catch (_: Exception) {
        null
    }
