package com.unde.library

import com.unde.library.internal.proxy.network.client.HttpClientWrapper
import com.unde.library.external.network.interceptor.ktor.UndeHttpInterceptor
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.test.Test
import kotlin.test.assertTrue

class UndeLibraryTest {
    @Test
    fun checkOkhttpRequest() {
        val classUnderTest = UndeLibrary()
        assertTrue(true, "someLibraryMethod should return 'true'")
        runBlocking {
            val okhttp = OkHttpClient.Builder().addNetworkInterceptor(UndeHttpInterceptor()).build()
            val responseOkhttp = okhttp.newCall(Request.Builder().url("https://google.com").get().build()).execute()
            assertTrue { responseOkhttp.body != null }
            val client = HttpClientWrapper.get()
            val response = client.get("https://google.com")
            assertTrue { response.body<HttpResponse>().toString() != null }
        }
        Thread.sleep(3000)
    }
}