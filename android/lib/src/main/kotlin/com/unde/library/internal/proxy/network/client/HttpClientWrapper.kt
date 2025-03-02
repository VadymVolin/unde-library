package com.unde.library.internal.proxy.network.client

import com.unde.library.internal.constants.DEFAULT_CONNECTION_TIMEOUT_SEC
import com.unde.library.internal.constants.DEFAULT_PING_INTERVAL_SEC
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.time.Duration

internal object HttpClientWrapper {
    private val httpClient = HttpClient(OkHttp) {
        engine {
            // TODO: The pingInterval and pingIntervalMillis properties are not applicable for the OkHttp engine
            //  https://ktor.io/docs/client-websockets.html#configure_plugin
            preconfigured = OkHttpClient.Builder()
                .pingInterval(Duration.ofSeconds(DEFAULT_PING_INTERVAL_SEC))
                .connectTimeout(Duration.ofSeconds(DEFAULT_CONNECTION_TIMEOUT_SEC))
                .build()
        }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
            maxFrameSize = Long.MAX_VALUE
        }
    }

    internal fun get() = httpClient
}