package app.homeflix.tv

import android.content.Context
import app.homeflix.tv.core.network.JellyfinClient
import app.homeflix.tv.core.network.normalizeServerUrl
import app.homeflix.tv.core.network.probeJellyfinServer
import app.homeflix.tv.core.session.ServerStore
import app.homeflix.tv.core.session.SessionStore
import app.homeflix.tv.core.session.SessionValidator
import app.homeflix.tv.core.session.StoredSession
import app.homeflix.tv.feature.auth.AuthApi
import app.homeflix.tv.feature.auth.AuthGateway
import app.homeflix.tv.feature.auth.ServerConnectError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.time.Duration
import java.util.UUID

private val PLAYBACK_CONNECT_TIMEOUT = Duration.ofSeconds(30)
private val PLAYBACK_RESOLVE_READ_TIMEOUT = Duration.ofMinutes(5)
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

    data class NeedsServer(
        val initialUrl: String,
    ) : BootstrapState

    data class Unavailable(
        val server: String,
    ) : BootstrapState

    data class Ready(
        val runtime: AppRuntime,
        val session: StoredSession?,
    ) : BootstrapState
}

internal suspend fun bootstrap(
    server: String?,
    deviceId: String,
    sessionStore: SessionStore,
    ioDispatcher: CoroutineDispatcher,
    probe: (String) -> Boolean = ::probeJellyfinServer,
): BootstrapState =
    withContext(ioDispatcher) {
        if (server == null) {
            return@withContext BootstrapState.NeedsServer(initialUrl = "")
        }
        if (!probe(server)) {
            return@withContext BootstrapState.Unavailable(server)
        }
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

internal suspend fun connectServer(
    input: String,
    serverStore: ServerStore,
    sessionStore: SessionStore,
    ioDispatcher: CoroutineDispatcher,
    probe: (String) -> Boolean = ::probeJellyfinServer,
): ServerConnectError? =
    withContext(ioDispatcher) {
        val url = normalizeServerUrl(input) ?: return@withContext ServerConnectError.InvalidUrl
        if (!probe(url)) {
            return@withContext ServerConnectError.Unreachable
        }
        if (url != serverStore.load()) {
            sessionStore.clear()
        }
        serverStore.save(url)
        null
    }

internal fun injectedRuntime(
    server: String,
    deviceId: String,
    authGateway: AuthGateway,
    sessionStore: SessionStore,
): AppRuntime =
    AppRuntime(
        server = server,
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

internal fun playbackClient(
    runtime: AppRuntime,
    session: StoredSession,
    ioDispatcher: CoroutineDispatcher,
): JellyfinClient =
    JellyfinClient(
        baseUrl = runtime.server,
        deviceId = runtime.deviceId,
        version = BuildConfig.VERSION_NAME,
        token = session.accessToken,
        callFactory =
            OkHttpClient
                .Builder()
                .connectTimeout(PLAYBACK_CONNECT_TIMEOUT)
                .readTimeout(PLAYBACK_RESOLVE_READ_TIMEOUT)
                .build(),
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
