package app.homeflix.tv.core.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AndroidServerStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val store = AndroidServerStore(context)

    @Before
    fun setUp() {
        store.clear()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun shouldRoundTripServerUrl() {
        store.save("http://server:8096")

        assertEquals("http://server:8096", store.load())
    }

    @Test
    fun shouldReturnNullAfterClear() {
        store.save("http://server:8096")

        store.clear()

        assertNull(store.load())
    }
}
