package com.unde.library

import com.unde.library.external.network.interceptor.ktor.UndeHttpSendInterceptor
import com.unde.library.external.network.interceptor.okhttp.UndeHttpInterceptor
import com.unde.library.internal.constants.DEFAULT_CONNECTION_TIMEOUT_SEC
import com.unde.library.internal.constants.DEFAULT_PING_INTERVAL_SEC
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.time.Duration
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class UndeLibraryTest {

    val okHttpClient = HttpClient(OkHttp) {
        engine {
            // The pingInterval and pingIntervalMillis properties are not applicable for the OkHttp engine
            //  https://ktor.io/docs/client-websockets.html#configure_plugin
            preconfigured = OkHttpClient.Builder()
                .pingInterval(Duration.ofSeconds(DEFAULT_PING_INTERVAL_SEC))
                .connectTimeout(Duration.ofSeconds(DEFAULT_CONNECTION_TIMEOUT_SEC))
                .addNetworkInterceptor(UndeHttpInterceptor())
                .build()
        }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
            maxFrameSize = Long.MAX_VALUE
        }
    }

    val interceptor = UndeHttpSendInterceptor()
    val ktorHttpClient = HttpClient(OkHttp) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
            maxFrameSize = Long.MAX_VALUE
        }
    }.apply { plugin(HttpSend).intercept(interceptor::invoke) }

    @Test
    fun checkOkhttpRequestInterceptor() {
        runBlocking {
            println("checkOkhttpRequestInterceptor:")
            val responseOkhttp = okHttpClient.get("https://www.google.com").body<String>()
            println("checkOkhttpRequestInterceptor: ${responseOkhttp.length}")
        }
        Thread.sleep(5L.seconds.inWholeMilliseconds)
    }

    @Test
    fun checkKtorRequestInterceptor() {
        runBlocking {
            println("checkKtorRequestInterceptor:")
            val responseKtor = ktorHttpClient.get("https://www.google.com").body<String>()
            println("checkKtorRequestInterceptor: ${responseKtor.length}")
        }
        Thread.sleep(5L.seconds.inWholeMilliseconds)
    }
}