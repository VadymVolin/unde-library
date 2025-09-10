package com.unde.library.internal.plugin.network.model

import kotlinx.serialization.Serializable

@Serializable
internal data class UndeRequest(
    val url: String,
    val method: String,
    val headers: Map<String, List<String>>,
    val body: String? = null
)
