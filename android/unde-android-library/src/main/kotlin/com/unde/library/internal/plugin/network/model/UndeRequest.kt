package com.unde.library.internal.plugin.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UndeRequest(
    @SerialName("url")
    val url: String,
    @SerialName("method")
    val method: String,
    @SerialName("headers")
    val headers: Map<String, List<String>>,
    @SerialName("body")
    val body: String? = null
)
