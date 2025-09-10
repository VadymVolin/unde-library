package com.unde.library.internal.plugin.network.model

import kotlinx.serialization.Serializable

@Serializable
internal data class UndeRequestResponse(val request: UndeRequest, val response: UndeResponse)
