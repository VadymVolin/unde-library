package com.unde.library

import com.unde.library.external.network.interceptor.okhttp.UndeHttpInterceptor
import com.unde.library.internal.constants.DEFAULT_CONNECTION_TIMEOUT_SEC
import com.unde.library.internal.constants.DEFAULT_PING_INTERVAL_SEC
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.BeforeClass
import org.junit.Test
import java.time.Duration
import kotlin.time.Duration.Companion.seconds
import com.unde.library.external.network.interceptor.ktor.UndeHttpInterceptor as KtorInterceptor

class UndeLibraryTest {

    companion object {
        @get:BeforeClass
        val undeLibrary = UndeLibrary()

        @get:BeforeClass
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

        @get:BeforeClass
        val interceptor = KtorInterceptor()
        @get:BeforeClass
        val ktorHttpClient = HttpClient(OkHttp) {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
                maxFrameSize = Long.MAX_VALUE
            }
        }.apply { plugin(HttpSend).intercept(interceptor::invoke) }
    }


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