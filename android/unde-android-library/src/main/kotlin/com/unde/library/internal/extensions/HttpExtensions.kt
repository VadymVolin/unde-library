package com.unde.library.internal.extensions

import com.unde.library.internal.plugin.network.model.UndeRequest
import com.unde.library.internal.plugin.network.model.UndeResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.util.GZipEncoder
import io.ktor.util.toMap
import java.util.zip.GZIPInputStream

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
    this.bodyAsText()
)

internal fun okhttp3.Request.toUndeRequest() = UndeRequest(
    this.url.toString(),
    this.method,
    this.headers.toMultimap(),
    // TODO: Not a real request body
    this.body?.toString()
)

internal fun okhttp3.Response.toUndeResponse() = UndeResponse(
    this.code,
    this.message,
    this.headers.toMultimap(),
    this.protocol.toString(),
    // TODO: Not a real response body 
    this.readOkHttpBody()
)

private fun okhttp3.Response.readOkHttpBody(): String {
    val peeked = this.peekBody(Long.MAX_VALUE)

    val isGzip = this.header(HttpHeaders.ContentEncoding) == GZipEncoder.name
            || this.header(HttpHeaders.ContentEncoding.lowercase()) == GZipEncoder.name
    val data = if (isGzip) {
        try {
            GZIPInputStream(peeked.byteStream()).use { it.readBytes() }
        } catch (e: Exception) {
            return peeked.string()
        }
    } else {
        return peeked.string()
    }

    val charset = this.body.contentType()?.charset() ?: Charsets.UTF_8
    return data.toString(charset)
}