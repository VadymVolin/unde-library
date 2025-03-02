package com.unde.library.external.network.interceptor.ktor

import com.unde.library.internal.extensions.toUndeRequest
import com.unde.library.internal.extensions.toUndeResponse
import com.unde.library.internal.plugin.network.UndeNetworkPlugin
import com.unde.library.internal.plugin.network.model.UndeRequestResponse
import okhttp3.Interceptor
import okhttp3.Response

class UndeHttpInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        println("Request: $request")
        val response = chain.proceed(request)
        println("Response: $response")
        UndeNetworkPlugin.handleRequest(UndeRequestResponse(request.toUndeRequest(), response.toUndeResponse()))
        return response
    }
}