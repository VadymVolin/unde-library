package com.unde.library.internal.plugin.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Container holding both request and response data.
 *
 * This is the primary object serialized and sent to the server or cached.
 */
@Serializable
@SerialName("data")
internal data class UndeRequestResponse(
    /**
     * The [UndeRequest] details.
     */
    @SerialName("request")
    val request: UndeRequest,

    /**
     * The [UndeResponse] details.
     */
    @SerialName("response")
    val response: UndeResponse
)
