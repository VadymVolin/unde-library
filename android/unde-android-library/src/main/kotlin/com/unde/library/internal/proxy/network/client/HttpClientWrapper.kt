package com.unde.library.internal.proxy.network.client

import com.unde.library.internal.constants.DEFAULT_CONNECTION_TIMEOUT_SEC
import com.unde.library.internal.constants.DEFAULT_PING_INTERVAL_SEC
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import kotlin.time.Duration.Companion.seconds

/**
 * Internal wrapper around [HttpClient] (Ktor with OkHttp engine).
 *
 * Configured with specific connection timeouts and ping intervals for internal use
 * (potentially for communication with the desktop server if bidirectional HTTP is used, though `ServerSocketProxy` handles main traffic).
 */
internal object HttpClientWrapper {
    /**
     * Singleton instance of the Ktor [HttpClient].
     */
    private val httpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = OkHttpClient.Builder()
                .pingInterval(DEFAULT_PING_INTERVAL_SEC)
                .connectTimeout(DEFAULT_CONNECTION_TIMEOUT_SEC)
                .build()
        }
    }

    /**
     * Retrieves the configured [HttpClient] instance.
     *
     * @return The singleton [HttpClient].
     */
    internal fun get() = httpClient
}