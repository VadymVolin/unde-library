package com.unde.library.external.network.interceptor.okhttp

import com.unde.library.internal.extensions.toUndeRequest
import com.unde.library.internal.extensions.toUndeResponse
import com.unde.library.internal.plugin.network.UndeNetworkPlugin
import com.unde.library.internal.plugin.network.model.UndeRequestResponse
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor for capturing and handling network requests via the Unde Library.
 *
 * This interceptor captures the outgoing request and the incoming response, passing them
 * to the [UndeNetworkPlugin] for processing (e.g., logging, caching, or forwarding to a connected tool).
 *
 * Usage:
 * ```
 * val client = OkHttpClient.Builder()
 *     .addInterceptor(UndeHttpInterceptor())
 *     .build()
 * ```
 */
class UndeHttpInterceptor : Interceptor {
    /**
     * Intercepts the network chain, processing the request and response.
     *
     * @param chain The [Interceptor.Chain] which provides access to the request and response.
     * @return The [Response] from the server (or downstream interceptors).
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        UndeNetworkPlugin.handleRequest(UndeRequestResponse(request.toUndeRequest(response.sentRequestAtMillis), response.toUndeResponse()))
        return response
    }
}