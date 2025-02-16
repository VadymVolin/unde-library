package com.unde.library.ktor.interceptor

import com.unde.library.plugin.network.UndeNetworkPlugin
import okhttp3.Interceptor
import okhttp3.Response

class UndeHttpInterceptor : Interceptor {

    private val networkPlugin = UndeNetworkPlugin

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        println("Request: $request")
        val response = chain.proceed(request)
        println("Response: $response")
        return response
    }
}