package app.homeflix.tv

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.homeflix.tv.core.catalog.LibrarySummary
import app.homeflix.tv.core.designsystem.TvNavProfile
import app.homeflix.tv.core.session.StoredSession
import app.homeflix.tv.feature.auth.AuthScreen
import app.homeflix.tv.feature.auth.AuthenticatedUser
import app.homeflix.tv.feature.detail.DetailApi
import app.homeflix.tv.feature.detail.DetailGateway
import app.homeflix.tv.feature.detail.DetailScreen
import app.homeflix.tv.feature.home.HomeApi
import app.homeflix.tv.feature.home.HomeGateway
import app.homeflix.tv.feature.home.HomeScreen
import app.homeflix.tv.feature.home.HomeViewer
import app.homeflix.tv.feature.library.LibraryApi
import app.homeflix.tv.feature.library.LibraryGateway
import app.homeflix.tv.feature.library.LibraryScreen
import app.homeflix.tv.feature.profile.ProfileDetails
import app.homeflix.tv.feature.profile.ProfileScreen
import app.homeflix.tv.feature.profile.profileServerAddress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PROFILE_IMAGE_QUALITY = 90

private sealed interface AuthenticatedDestination {
    data object Home : AuthenticatedDestination

    data class Library(
        val library: LibrarySummary,
    ) : AuthenticatedDestination

    data object Profile : AuthenticatedDestination
}

@Composable
internal fun AuthenticatedContent(
    runtime: AppRuntime,
    restoredSession: StoredSession?,
    onPlaySelected: (String) -> Unit,
    homeGateway: HomeGateway?,
    libraryGateway: LibraryGateway?,
    detailGateway: DetailGateway?,
    ioDispatcher: CoroutineDispatcher,
) {
    var session by remember(runtime, restoredSession) { mutableStateOf(restoredSession) }
    val scope = rememberCoroutineScope()
    val activeSession = session

    if (activeSession == null) {
        AuthScreen(
            gateway = runtime.authGateway,
            onAuthenticated = { user ->
                scope.launch {
                    val storedSession = user.toStoredSession()
                    withContext(ioDispatcher) { runtime.sessionStore.save(storedSession) }
                    session = storedSession
                }
            },
        )
        return
    }

    SignedInContent(
        runtime = runtime,
        session = activeSession,
        onPlaySelected = onPlaySelected,
        homeGateway = homeGateway,
        libraryGateway = libraryGateway,
        detailGateway = detailGateway,
        ioDispatcher = ioDispatcher,
        onSignOut = {
            scope.launch {
                withContext(ioDispatcher) { runtime.sessionStore.clear() }
                session = null
            }
        },
    )
}

@Composable
private fun SignedInContent(
    runtime: AppRuntime,
    session: StoredSession,
    onPlaySelected: (String) -> Unit,
    homeGateway: HomeGateway?,
    libraryGateway: LibraryGateway?,
    detailGateway: DetailGateway?,
    ioDispatcher: CoroutineDispatcher,
    onSignOut: () -> Unit,
) {
    val activeSession = session
    val gateways =
        rememberGateways(runtime, activeSession, homeGateway, libraryGateway, detailGateway, ioDispatcher)
    var detailStack by remember(activeSession) { mutableStateOf(emptyList<String>()) }
    val openDetail: (String) -> Unit = { itemId -> detailStack = detailStack + itemId }
    var libraries by remember(gateways, activeSession) {
        mutableStateOf(emptyList<LibrarySummary>())
    }
    var destination by remember(activeSession) {
        mutableStateOf<AuthenticatedDestination>(AuthenticatedDestination.Home)
    }

    LaunchedEffect(gateways, activeSession) {
        libraries = loadLibraries(gateways.library, activeSession.userId)
    }

    val viewer = runtime.viewer(activeSession)

    val activeDetailItem = detailStack.lastOrNull()
    if (activeDetailItem != null) {
        DetailDestination(
            gateway = gateways.detail,
            userId = activeSession.userId,
            itemId = activeDetailItem,
            viewer = viewer,
            libraries = libraries,
            onBack = { detailStack = detailStack.dropLast(1) },
            onDestinationSelected = { selected ->
                detailStack = emptyList()
                destination = selected
            },
            onMediaSelected = openDetail,
            onPlaySelected = onPlaySelected,
        )
        return
    }

    BaseDestination(
        destination = destination,
        runtime = runtime,
        userId = activeSession.userId,
        viewer = viewer,
        libraries = libraries,
        homeGateway = gateways.home,
        libraryGateway = gateways.library,
        ioDispatcher = ioDispatcher,
        onDestinationSelected = { selected -> destination = selected },
        onMediaSelected = openDetail,
        onSignOut = onSignOut,
    )
}

private class SignedInGateways(
    val home: HomeGateway,
    val library: LibraryGateway,
    val detail: DetailGateway,
)

@Composable
private fun rememberGateways(
    runtime: AppRuntime,
    session: StoredSession,
    homeGateway: HomeGateway?,
    libraryGateway: LibraryGateway?,
    detailGateway: DetailGateway?,
    ioDispatcher: CoroutineDispatcher,
): SignedInGateways {
    val client =
        remember(runtime, session, ioDispatcher) {
            authenticatedClient(runtime, session, ioDispatcher)
        }
    return remember(runtime, client, homeGateway, libraryGateway, detailGateway) {
        SignedInGateways(
            home = homeGateway ?: HomeApi(baseUrl = runtime.server, client = client),
            library = libraryGateway ?: LibraryApi(baseUrl = runtime.server, client = client),
            detail = detailGateway ?: DetailApi(baseUrl = runtime.server, client = client),
        )
    }
}

@Composable
private fun DetailDestination(
    gateway: DetailGateway,
    userId: String,
    itemId: String,
    viewer: HomeViewer,
    libraries: List<LibrarySummary>,
    onBack: () -> Unit,
    onDestinationSelected: (AuthenticatedDestination) -> Unit,
    onMediaSelected: (String) -> Unit,
    onPlaySelected: (String) -> Unit,
) {
    BackHandler(onBack = onBack)
    DetailScreen(
        gateway = gateway,
        userId = userId,
        itemId = itemId,
        profile = TvNavProfile(name = viewer.name, avatarUrl = viewer.avatarUrl),
        libraries = libraries,
        onHomeSelected = { onDestinationSelected(AuthenticatedDestination.Home) },
        onLibrarySelected = { library ->
            onDestinationSelected(AuthenticatedDestination.Library(library))
        },
        onProfileSelected = { onDestinationSelected(AuthenticatedDestination.Profile) },
        onMediaSelected = onMediaSelected,
        onPlaySelected = onPlaySelected,
    )
}

@Composable
private fun BaseDestination(
    destination: AuthenticatedDestination,
    runtime: AppRuntime,
    userId: String,
    viewer: HomeViewer,
    libraries: List<LibrarySummary>,
    homeGateway: HomeGateway,
    libraryGateway: LibraryGateway,
    ioDispatcher: CoroutineDispatcher,
    onDestinationSelected: (AuthenticatedDestination) -> Unit,
    onMediaSelected: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    when (destination) {
        AuthenticatedDestination.Home ->
            HomeScreen(
                gateway = homeGateway,
                viewer = viewer,
                onMediaSelected = onMediaSelected,
                onProfileSelected = { onDestinationSelected(AuthenticatedDestination.Profile) },
                libraries = libraries,
                onLibrarySelected = { library ->
                    onDestinationSelected(AuthenticatedDestination.Library(library))
                },
            )

        is AuthenticatedDestination.Library -> {
            BackHandler {
                onDestinationSelected(AuthenticatedDestination.Home)
            }
            LibraryScreen(
                gateway = libraryGateway,
                userId = userId,
                library = destination.library,
                libraries = libraries,
                profile = TvNavProfile(name = viewer.name, avatarUrl = viewer.avatarUrl),
                onHomeSelected = { onDestinationSelected(AuthenticatedDestination.Home) },
                onLibrarySelected = { library ->
                    onDestinationSelected(AuthenticatedDestination.Library(library))
                },
                onMediaSelected = onMediaSelected,
                onProfileSelected = { onDestinationSelected(AuthenticatedDestination.Profile) },
            )
        }

        AuthenticatedDestination.Profile -> {
            BackHandler {
                onDestinationSelected(AuthenticatedDestination.Home)
            }
            ProfileScreen(
                details =
                    ProfileDetails(
                        name = viewer.name,
                        avatarUrl = viewer.avatarUrl,
                        serverAddress = profileServerAddress(runtime.server),
                        appVersion = BuildConfig.VERSION_NAME,
                    ),
                profile = TvNavProfile(name = viewer.name, avatarUrl = viewer.avatarUrl),
                libraries = libraries,
                onHomeSelected = { onDestinationSelected(AuthenticatedDestination.Home) },
                onLibrarySelected = { library ->
                    onDestinationSelected(AuthenticatedDestination.Library(library))
                },
                onSwitchProfile = onSignOut,
                ioDispatcher = ioDispatcher,
            )
        }
    }
}

private suspend fun loadLibraries(
    gateway: LibraryGateway,
    userId: String,
): List<LibrarySummary> =
    try {
        gateway.fetchLibraries(userId)
    } catch (failure: CancellationException) {
        throw failure
    } catch (_: Exception) {
        emptyList()
    }

private fun AuthenticatedUser.toStoredSession(): StoredSession =
    StoredSession(
        accessToken = accessToken,
        userId = userId,
        userName = userName,
        primaryImageTag = primaryImageTag,
    )

private fun AppRuntime.viewer(session: StoredSession): HomeViewer =
    HomeViewer(
        id = session.userId,
        name = session.userName,
        avatarUrl =
            session.primaryImageTag?.let { imageTag ->
                "${server.trimEnd('/')}/Users/${session.userId}/Images/Primary" +
                    "?tag=$imageTag&quality=$PROFILE_IMAGE_QUALITY"
            },
    )
