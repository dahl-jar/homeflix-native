package app.homeflix.tv.core.network

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

private const val PUBLIC_SYSTEM_PATH = "/System/Info/Public"

fun probeJellyfinServer(
    baseUrl: String,
    callFactory: Call.Factory = OkHttpClient(),
): Boolean =
    runCatching {
        val request = Request.Builder().url(baseUrl.trimEnd('/') + PUBLIC_SYSTEM_PATH).build()
        callFactory.newCall(request).execute().use { response -> response.isSuccessful }
    }.getOrDefault(false)
