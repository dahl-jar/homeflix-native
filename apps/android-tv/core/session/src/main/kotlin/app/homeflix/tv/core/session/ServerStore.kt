package app.homeflix.tv.core.session

import android.content.Context

interface ServerStore {
    fun save(url: String)

    fun load(): String?

    fun clear()
}

class AndroidServerStore(
    context: Context,
) : ServerStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun save(url: String) {
        check(preferences.edit().putString(SERVER_URL_KEY, url).commit()) { "Unable to persist server" }
    }

    override fun load(): String? = preferences.getString(SERVER_URL_KEY, null)

    override fun clear() {
        check(preferences.edit().remove(SERVER_URL_KEY).commit()) { "Unable to clear server" }
    }

    private companion object {
        const val PREFERENCES_NAME = "homeflix-server"
        const val SERVER_URL_KEY = "server-url"
    }
}
