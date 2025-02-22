package com.unde.library.plugin.network

import com.unde.library.plugin.network.model.UndeRequest
import com.unde.library.plugin.network.model.UndeResponse

internal object UndeNetworkPlugin {

    fun handleRequest(undeRequest: UndeRequest, undeResponse: UndeResponse) {
        println("UndeRequest: $undeRequest")
        println("UndeResponse: $undeResponse")
    }

}