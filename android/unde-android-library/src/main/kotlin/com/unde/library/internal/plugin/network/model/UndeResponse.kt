package com.unde.library.internal.plugin.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UndeResponse(
    @SerialName("responseTime")
    val responseTime: Long,
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String,
    @SerialName("headers")
    val headers: Map<String, List<String>>,
    @SerialName("protocol")
    val protocol: String,
    @SerialName("body")
    val body: String?
)
