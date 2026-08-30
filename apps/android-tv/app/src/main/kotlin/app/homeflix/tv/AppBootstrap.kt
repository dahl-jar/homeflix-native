package app.homeflix.tv

import android.content.Context
import app.homeflix.tv.core.network.JellyfinClient
import app.homeflix.tv.core.network.parseServerCandidates
import app.homeflix.tv.core.network.probeJellyfinServer
import app.homeflix.tv.core.network.resolveServer
import app.homeflix.tv.core.session.SessionStore
import app.homeflix.tv.core.session.SessionValidator
import app.homeflix.tv.core.session.StoredSession
import app.homeflix.tv.feature.auth.AuthApi
import app.homeflix.tv.feature.auth.AuthGateway
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.UUID

private const val DEVICE_PREFERENCES = "homeflix-device"
private const val DEVICE_ID_KEY = "device-id"

internal data class AppRuntime(
    val server: String,
    val deviceId: String,
    val authGateway: AuthGateway,
    val sessionStore: SessionStore,
)

internal sealed interface BootstrapState {
    data object Loading : BootstrapState

    data object Unavailable : BootstrapState

    data class Ready(
        val runtime: AppRuntime,
        val session: StoredSession?,
    ) : BootstrapState
}

internal suspend fun bootstrap(
    serverUrls: String,
    deviceId: String,
    sessionStore: SessionStore,
    ioDispatcher: CoroutineDispatcher,
): BootstrapState =
    withContext(ioDispatcher) {
        val server =
            resolveServer(parseServerCandidates(serverUrls), ::probeJellyfinServer)
                ?: return@withContext BootstrapState.Unavailable
        val authGateway = AuthApi(server, publicClient(server, deviceId, ioDispatcher))
        val runtime = AppRuntime(server, deviceId, authGateway, sessionStore)
        val storedSession = sessionStore.load()
        val restoredSession =
            storedSession?.takeIf { session ->
                SessionValidator.validate(authenticatedClient(runtime, session, ioDispatcher))
            }
        if (storedSession != null && restoredSession == null) {
            sessionStore.clear()
        }
        BootstrapState.Ready(runtime, restoredSession)
    }

internal fun injectedRuntime(
    serverUrls: String,
    deviceId: String,
    authGateway: AuthGateway,
    sessionStore: SessionStore,
): AppRuntime =
    AppRuntime(
        server = parseServerCandidates(serverUrls).firstOrNull().orEmpty(),
        deviceId = deviceId,
        authGateway = authGateway,
        sessionStore = sessionStore,
    )

internal fun authenticatedClient(
    runtime: AppRuntime,
    session: StoredSession,
    ioDispatcher: CoroutineDispatcher,
): JellyfinClient =
    JellyfinClient(
        baseUrl = runtime.server,
        deviceId = runtime.deviceId,
        version = BuildConfig.VERSION_NAME,
        token = session.accessToken,
        ioDispatcher = ioDispatcher,
    )

internal fun loadOrCreateDeviceId(context: Context): String {
    val preferences = context.getSharedPreferences(DEVICE_PREFERENCES, Context.MODE_PRIVATE)
    return preferences.getString(DEVICE_ID_KEY, null)
        ?: UUID.randomUUID().toString().also { deviceId ->
            preferences.edit().putString(DEVICE_ID_KEY, deviceId).apply()
        }
}

private fun publicClient(
    server: String,
    deviceId: String,
    ioDispatcher: CoroutineDispatcher,
): JellyfinClient =
    JellyfinClient(
        baseUrl = server,
        deviceId = deviceId,
        version = BuildConfig.VERSION_NAME,
        ioDispatcher = ioDispatcher,
    )
