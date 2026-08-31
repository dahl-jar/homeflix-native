package app.homeflix.tv.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

private const val DEFAULT_MAX_RETRIES = 2
private const val DEFAULT_RETRY_DELAY_MILLIS = 400L
private const val SERVER_ERROR_RANGE_START = 500

class RetryInterceptor(
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
    private val retryDelayMillis: Long = DEFAULT_RETRY_DELAY_MILLIS,
    private val sleep: (Long) -> Unit = Thread::sleep,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        while (true) {
            try {
                val response = chain.proceed(chain.request())
                if (response.code < SERVER_ERROR_RANGE_START || attempt == maxRetries) {
                    return response
                }
                response.close()
            } catch (failure: IOException) {
                if (attempt == maxRetries) throw failure
            }
            attempt += 1
            sleep(retryDelayMillis)
        }
    }
}
