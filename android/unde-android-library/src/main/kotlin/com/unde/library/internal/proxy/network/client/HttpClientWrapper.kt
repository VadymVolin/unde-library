package com.unde.library.internal.proxy.network.client

import com.unde.library.internal.constants.DEFAULT_CONNECTION_TIMEOUT_SEC
import com.unde.library.internal.constants.DEFAULT_PING_INTERVAL_SEC
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import kotlin.time.Duration.Companion.seconds

internal object HttpClientWrapper {
    private val httpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = OkHttpClient.Builder()
                .pingInterval(DEFAULT_PING_INTERVAL_SEC)
                .connectTimeout(DEFAULT_CONNECTION_TIMEOUT_SEC)
                .build()
        }
    }

    internal fun get() = httpClient
}