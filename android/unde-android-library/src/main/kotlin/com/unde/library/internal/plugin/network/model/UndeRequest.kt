package com.unde.library.internal.plugin.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable model representing an intercepted network request.
 */
@Serializable
internal data class UndeRequest(
    /**
     * Timestamp of when the request was sent.
     */
    @SerialName("requestTime")
    val requestTime: Long,

    /**
     * The full URL of the request.
     */
    @SerialName("url")
    val url: String,

    /**
     * HTTP method (GET, POST, etc.).
     */
    @SerialName("method")
    val method: String,

    /**
     * Map of request headers.
     */
    @SerialName("headers")
    val headers: Map<String, List<String>>,

    /**
     * Request body as a string, if available.
     */
    @SerialName("body")
    val body: String? = null
)
