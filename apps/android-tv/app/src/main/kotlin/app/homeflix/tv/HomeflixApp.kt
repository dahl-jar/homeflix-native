package app.homeflix.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.homeflix.tv.core.designsystem.HomeflixTheme
import app.homeflix.tv.core.session.AndroidServerStore
import app.homeflix.tv.core.session.AndroidSessionStore
import app.homeflix.tv.feature.auth.AuthGateway
import app.homeflix.tv.feature.auth.AuthLoadingScreen
import app.homeflix.tv.feature.auth.AuthUnavailableScreen
import app.homeflix.tv.feature.auth.ServerSetupScreen
import app.homeflix.tv.feature.detail.DetailGateway
import app.homeflix.tv.feature.home.HomeGateway
import app.homeflix.tv.feature.library.LibraryGateway
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeflixApp(
    server: String? = null,
    authGateway: AuthGateway? = null,
    homeGateway: HomeGateway? = null,
    libraryGateway: LibraryGateway? = null,
    detailGateway: DetailGateway? = null,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val context = LocalContext.current.applicationContext
    val deviceId = remember(context) { loadOrCreateDeviceId(context) }
    val sessionStore = remember(context) { AndroidSessionStore(context) }
    val serverStore = remember(context) { AndroidServerStore(context) }
    var state by remember(server, authGateway) {
        mutableStateOf<BootstrapState>(BootstrapState.Loading)
    }
    var bootstrapKey by remember(server, authGateway) { mutableIntStateOf(0) }
    val rebootstrap = {
        state = BootstrapState.Loading
        bootstrapKey += 1
    }

    LaunchedEffect(server, authGateway, deviceId, bootstrapKey) {
        state =
            if (authGateway == null) {
                val storedServer = server ?: withContext(ioDispatcher) { serverStore.load() }
                bootstrap(storedServer, deviceId, sessionStore, ioDispatcher)
            } else {
                BootstrapState.Ready(
                    runtime = injectedRuntime(server.orEmpty(), deviceId, authGateway, sessionStore),
                    session = null,
                )
            }
    }

    HomeflixTheme {
        when (val current = state) {
            BootstrapState.Loading -> AuthLoadingScreen()
            is BootstrapState.NeedsServer ->
                ServerSetupScreen(
                    initialUrl = current.initialUrl,
                    onConnect = { input ->
                        val error = connectServer(input, serverStore, sessionStore, ioDispatcher)
                        if (error == null) {
                            rebootstrap()
                        }
                        error
                    },
                    onBack = if (current.initialUrl.isEmpty()) null else rebootstrap,
                )
            is BootstrapState.Unavailable ->
                AuthUnavailableScreen(
                    onRetry = rebootstrap,
                    onChangeServer = { state = BootstrapState.NeedsServer(current.server) },
                )
            is BootstrapState.Ready ->
                AuthenticatedContent(
                    runtime = current.runtime,
                    restoredSession = current.session,
                    homeGateway = homeGateway,
                    libraryGateway = libraryGateway,
                    detailGateway = detailGateway,
                    ioDispatcher = ioDispatcher,
                    onChangeServer = { state = BootstrapState.NeedsServer(current.runtime.server) },
                )
        }
    }
}
