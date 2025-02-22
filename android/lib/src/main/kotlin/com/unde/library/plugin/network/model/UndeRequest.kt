package com.unde.library.plugin.network.model

data class UndeRequest(
    val url: String,
    val method: String,
    val headers: Map<String, List<String>>,
    val body: String? = null
)
