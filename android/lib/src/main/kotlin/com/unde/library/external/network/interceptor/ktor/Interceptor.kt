package com.unde.library.external.network.interceptor.ktor

import com.unde.library.internal.extensions.toUndeRequest
import com.unde.library.internal.extensions.toUndeResponse
import com.unde.library.internal.plugin.network.UndeNetworkPlugin
import com.unde.library.internal.plugin.network.model.UndeRequestResponse
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*

class UndeHttpSendInterceptor : HttpSendInterceptor {

    override suspend fun invoke(sender: Sender, requestBuilder: HttpRequestBuilder): HttpClientCall {
        return sender.execute(requestBuilder).also {
            val request = it.request
            val response = it.response
            UndeNetworkPlugin.handleRequest(UndeRequestResponse(request.toUndeRequest(), response.toUndeResponse()))
        }
    }
}

//class UndeHttpPipelineInterceptor: PipelineInterceptor<Any, HttpRequestBuilder> {
//    override suspend fun invoke(context: PipelineContext<Any, HttpRequestBuilder>, subject: Any) {
//        val request = context.context
//        println("Request: $request")
//        val response = context.subject
//        println("Response: $response")
//        UndeNetworkPlugin.handleRequest(UndeRequestResponse(request.build()..toUndeRequest(), response.toUndeResponse()))
//        context.proceed()
//    }
//}