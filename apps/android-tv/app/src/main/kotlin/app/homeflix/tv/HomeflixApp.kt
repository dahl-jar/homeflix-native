package app.homeflix.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.homeflix.tv.core.designsystem.HomeflixTheme
import app.homeflix.tv.core.session.AndroidSessionStore
import app.homeflix.tv.feature.auth.AuthGateway
import app.homeflix.tv.feature.auth.AuthLoadingScreen
import app.homeflix.tv.feature.auth.AuthUnavailableScreen
import app.homeflix.tv.feature.detail.DetailGateway
import app.homeflix.tv.feature.home.HomeGateway
import app.homeflix.tv.feature.library.LibraryGateway
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Composable
fun HomeflixApp(
    serverUrls: String = BuildConfig.HOMEFLIX_SERVER_URLS,
    authGateway: AuthGateway? = null,
    homeGateway: HomeGateway? = null,
    libraryGateway: LibraryGateway? = null,
    detailGateway: DetailGateway? = null,
    onPlaySelected: (String) -> Unit = {},
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val context = LocalContext.current.applicationContext
    val deviceId = remember(context) { loadOrCreateDeviceId(context) }
    val sessionStore = remember(context) { AndroidSessionStore(context) }
    var state by remember(serverUrls, authGateway) {
        mutableStateOf<BootstrapState>(BootstrapState.Loading)
    }

    LaunchedEffect(serverUrls, authGateway, deviceId) {
        state =
            if (authGateway == null) {
                bootstrap(serverUrls, deviceId, sessionStore, ioDispatcher)
            } else {
                BootstrapState.Ready(
                    runtime = injectedRuntime(serverUrls, deviceId, authGateway, sessionStore),
                    session = null,
                )
            }
    }

    HomeflixTheme {
        when (val current = state) {
            BootstrapState.Loading -> AuthLoadingScreen()
            BootstrapState.Unavailable -> AuthUnavailableScreen()
            is BootstrapState.Ready ->
                AuthenticatedContent(
                    runtime = current.runtime,
                    restoredSession = current.session,
                    onPlaySelected = onPlaySelected,
                    homeGateway = homeGateway,
                    libraryGateway = libraryGateway,
                    detailGateway = detailGateway,
                    ioDispatcher = ioDispatcher,
                )
        }
    }
}
