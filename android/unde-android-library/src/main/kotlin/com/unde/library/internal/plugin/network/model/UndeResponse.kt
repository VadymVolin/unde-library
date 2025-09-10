package com.unde.library.internal.plugin.network.model

import kotlinx.serialization.Serializable

@Serializable
internal data class UndeResponse(
    val code: Int,
    val message: String,
    val toMultimap: Map<String, List<String>>,
    val protocol: String,
    val body: String?
)
