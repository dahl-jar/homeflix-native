package app.homeflix.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.homeflix.tv.core.session.StoredSession
import app.homeflix.tv.feature.auth.AuthScreen
import app.homeflix.tv.feature.auth.AuthenticatedUser
import app.homeflix.tv.feature.home.HomeApi
import app.homeflix.tv.feature.home.HomeGateway
import app.homeflix.tv.feature.home.HomeScreen
import app.homeflix.tv.feature.home.HomeViewer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PROFILE_IMAGE_QUALITY = 90

@Composable
internal fun AuthenticatedContent(
    runtime: AppRuntime,
    restoredSession: StoredSession?,
    onMediaSelected: (String) -> Unit,
    homeGateway: HomeGateway?,
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

    val activeHomeGateway =
        remember(runtime, activeSession, ioDispatcher, homeGateway) {
            homeGateway
                ?: HomeApi(
                    baseUrl = runtime.server,
                    client = authenticatedClient(runtime, activeSession, ioDispatcher),
                )
        }
    HomeScreen(
        gateway = activeHomeGateway,
        viewer = runtime.viewer(activeSession),
        onMediaSelected = onMediaSelected,
        onProfileSelected = {
            scope.launch {
                withContext(ioDispatcher) { runtime.sessionStore.clear() }
                session = null
            }
        },
    )
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
