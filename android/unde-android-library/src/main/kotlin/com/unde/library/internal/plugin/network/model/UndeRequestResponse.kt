package com.unde.library.internal.plugin.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("data")
internal data class UndeRequestResponse(
    @SerialName("request")
    val request: UndeRequest,
    @SerialName("response")
    val response: UndeResponse
)
