package com.unde.library.internal.extensions

import com.unde.library.internal.plugin.network.model.UndeRequest
import com.unde.library.internal.plugin.network.model.UndeResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.util.GZipEncoder
import io.ktor.util.toMap
import okio.Buffer
import java.util.zip.GZIPInputStream

/**
 * Maximum size of the request/response body to read into memory (5MB).
 */
private const val DEFAULT_MAX_BODY_SIZE = 5 * 1024 * 1024L // 5MB

/**
 * Converts a Ktor [HttpRequest] to [UndeRequest].
 *
 * @param sentRequestAtMillis Timestamp when the request was initiated.
 * @return [UndeRequest] populated with request details.
 */
internal fun io.ktor.client.request.HttpRequest.toUndeRequest(sentRequestAtMillis: Long) = UndeRequest(
    sentRequestAtMillis,
    this.url.toString(),
    this.method.value,
    this.headers.toMap(),
    this.content.toString()
)

/**
 * Converts a Ktor [HttpResponse] to [UndeResponse].
 */
internal suspend fun io.ktor.client.statement.HttpResponse.toUndeResponse() = UndeResponse(
    this.responseTime.timestamp,
    this.status.value,
    this.status.description,
    this.headers.toMap(),
    this.version.toString(),
    this.bodyAsText()
)

/**
 * Converts an OkHttp [Request] to [UndeRequest].
 *
 * @param sentRequestAtMillis Timestamp when the request was initiated.
 * @return [UndeRequest] populated with request details.
 */
internal fun okhttp3.Request.toUndeRequest(sentRequestAtMillis: Long) = UndeRequest(
    sentRequestAtMillis,
    this.url.toString(),
    this.method,
    this.headers.toMultimap(),
    this.readOkHttpRequestBody()
)

/**
 * Converts an OkHttp [Response] to [UndeResponse].
 *
 * @return [UndeResponse] populated with response details and body.
 */
internal fun okhttp3.Response.toUndeResponse() = UndeResponse(
    this.receivedResponseAtMillis,
    this.code,
    this.message,
    this.headers.toMultimap(),
    this.protocol.toString(),
    this.readOkHttpResponseBody()
)

/**
 * Reads the OkHttp request body into a string, respecting a max size limit.
 *
 * @param maxSize Maximum number of bytes to read (default 5MB).
 * @return The body string or a placeholder if too large/null.
 */
private fun okhttp3.Request.readOkHttpRequestBody(maxSize: Long = DEFAULT_MAX_BODY_SIZE): String? {
    val body = this.body ?: return null
    
    val buffer = Buffer()
    body.writeTo(buffer)
    
    if (buffer.size > maxSize) {
        return "[Body too large: ${buffer.size} bytes]"
    }
    
    val charset = body.contentType()?.charset() ?: Charsets.UTF_8
    return buffer.readString(charset)
}

/**
 * Reads the OkHttp response body into a string, handling GZip encoding if present.
 *
 * Uses [peekBody] to avoid consuming the original response stream.
 *
 * @return The body string.
 */
private fun okhttp3.Response.readOkHttpResponseBody(): String {
    val peeked = this.peekBody(Long.MAX_VALUE)

    val isGzip = this.header(HttpHeaders.ContentEncoding) == GZipEncoder.name
            || this.header(HttpHeaders.ContentEncoding.lowercase()) == GZipEncoder.name
    val data = if (isGzip) {
        try {
            GZIPInputStream(peeked.byteStream()).use { it.readBytes() }
        } catch (_: Exception) {
            return peeked.string()
        }
    } else {
        return peeked.string()
    }

    val charset = this.body.contentType()?.charset() ?: Charsets.UTF_8
    return data.toString(charset)
}