package com.unde.library.plugin.network.model

data class UndeResponse(
    val code: Int,
    val message: String,
    val toMultimap: Map<String, List<String>>,
    val protocol: String,
    val body: String?
)
