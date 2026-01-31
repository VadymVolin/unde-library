package com.unde.library.internal.plugin.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable model representing an intercepted network response.
 */
@Serializable
internal data class UndeResponse(
    /**
     * Timestamp of when the response was received.
     */
    @SerialName("responseTime")
    val responseTime: Long,

    /**
     * HTTP status code.
     */
    @SerialName("code")
    val code: Int,

    /**
     * HTTP status message.
     */
    @SerialName("message")
    val message: String,

    /**
     * Map of response headers.
     */
    @SerialName("headers")
    val headers: Map<String, List<String>>,

    /**
     * HTTP protocol version.
     */
    @SerialName("protocol")
    val protocol: String,

    /**
     * Response body as a string, if available.
     */
    @SerialName("body")
    val body: String?
)
