package com.unde.library.internal.extensions

import com.unde.library.internal.plugin.network.model.UndeRequest
import com.unde.library.internal.plugin.network.model.UndeResponse
import io.ktor.client.call.*
import io.ktor.util.*

internal fun io.ktor.client.request.HttpRequest.toUndeRequest() = UndeRequest(
    this.url.toString(),
    this.method.value,
    this.headers.toMap(),
    this.content.toString()
)

internal suspend fun io.ktor.client.statement.HttpResponse.toUndeResponse() = UndeResponse(
    this.status.value,
    this.status.description,
    this.headers.toMap(),
    this.version.toString(),
    this.body<String>()
)

internal fun okhttp3.Request.toUndeRequest() = UndeRequest(
    this.url.toString(),
    this.method,
    this.headers.toMultimap(),
    this.body?.toString()
)

internal fun okhttp3.Response.toUndeResponse() = UndeResponse(
    this.code,
    this.message,
    this.headers.toMultimap(),
    this.protocol.toString(),
    // TODO: Not a real response body 
    this.body.toString()
)