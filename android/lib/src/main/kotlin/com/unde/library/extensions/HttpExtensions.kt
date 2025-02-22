package com.unde.library.extensions

import com.unde.library.plugin.network.model.UndeRequest
import com.unde.library.plugin.network.model.UndeResponse
import okhttp3.Request
import okhttp3.Response

fun Request.toUndeRequest() = UndeRequest(
    this.url.toString(),
    this.method,
    this.headers.toMultimap(),
    this.body?.toString()
)

fun Response.toUndeResponse() = UndeResponse(
    this.code,
    this.message,
    this.headers.toMultimap(),
    this.protocol.toString(),
    this.body?.toString()
)