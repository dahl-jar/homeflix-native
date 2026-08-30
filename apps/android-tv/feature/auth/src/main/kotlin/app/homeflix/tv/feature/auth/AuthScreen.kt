package app.homeflix.tv.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.HomeflixDimensions
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
            runCatching { gateway.fetchPublicProfiles() }
                .fold(
                    onSuccess = { profiles -> AuthContentState.Profiles(profiles) },
                    onFailure = { AuthContentState.Unavailable },
                )
    }

    when (val state = contentState) {
        AuthContentState.Loading -> AuthLoadingScreen(modifier)
        AuthContentState.Unavailable -> AuthUnavailableScreen(modifier)
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
    AuthStatusScreen(
        title = "Loading profiles…",
        detail = null,
        modifier = modifier,
    )
}

@Composable
fun AuthUnavailableScreen(modifier: Modifier = Modifier) {
    AuthStatusScreen(
        title = "Can’t reach the server.",
        detail = "Check the server configuration and network",
        modifier = modifier,
    )
}

@Composable
private fun AuthStatusScreen(
    title: String,
    detail: String?,
    modifier: Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .fillMaxSize()
                .background(HomeflixColors.Background),
    ) {
        Wordmark(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(HomeflixDimensions.WordmarkEdgePadding),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = HomeflixColors.OnBackground,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
            )
            detail?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = it,
                    color = HomeflixColors.Muted,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private suspend fun authenticate(
    gateway: AuthGateway,
    profile: AuthProfile,
    password: String,
): AuthenticatedUser? = runCatching { gateway.authenticate(profile.name, password) }.getOrNull()
