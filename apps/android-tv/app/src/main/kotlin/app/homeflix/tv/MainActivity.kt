package app.homeflix.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.homeflix.tv.core.network.RetryInterceptor
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SingletonImageLoader.setSafe { context ->
            ImageLoader
                .Builder(context)
                .memoryCache {
                    MemoryCache
                        .Builder()
                        .maxSizePercent(context, ImageMemoryBudget.MEMORY_CACHE_PERCENT)
                        .build()
                }.components {
                    add(
                        OkHttpNetworkFetcherFactory(
                            callFactory = {
                                OkHttpClient
                                    .Builder()
                                    .addInterceptor(RetryInterceptor())
                                    .build()
                            },
                        ),
                    )
                }.build()
        }
        setContent {
            HomeflixApp()
        }
    }
}
