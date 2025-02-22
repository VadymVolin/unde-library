package com.unde.library.ktor.interceptor

import com.unde.library.extensions.toUndeRequest
import com.unde.library.extensions.toUndeResponse
import com.unde.library.plugin.network.UndeNetworkPlugin
import okhttp3.Interceptor
import okhttp3.Response

class UndeHttpInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        println("Request: $request")
        val response = chain.proceed(request)
        println("Response: $response")
        UndeNetworkPlugin.handleRequest(request.toUndeRequest(), response.toUndeResponse())
        return response
    }
}