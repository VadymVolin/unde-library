package com.unde.library.internal.proxy.network.client

import com.unde.library.internal.constants.DEFAULT_CONNECTION_TIMEOUT_SEC
import com.unde.library.internal.constants.DEFAULT_PING_INTERVAL_SEC
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import kotlin.time.Duration.Companion.seconds

internal object HttpClientWrapper {
    private val httpClient = HttpClient(OkHttp) {
        engine {
            // The pingInterval and pingIntervalMillis properties are not applicable for the OkHttp engine
            //  https://ktor.io/docs/client-websockets.html#configure_plugin

            preconfigured = OkHttpClient.Builder()
                .pingInterval(DEFAULT_PING_INTERVAL_SEC.seconds)
                .connectTimeout(DEFAULT_CONNECTION_TIMEOUT_SEC.seconds)
                .build()
        }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
            maxFrameSize = Long.MAX_VALUE
        }
    }

    internal fun get() = httpClient
}