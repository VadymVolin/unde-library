package com.unde.library.internal.plugin.network

import com.unde.library.internal.plugin.network.model.UndeRequestResponse
import com.unde.library.internal.plugin.network.model.WSMessage
import com.unde.library.internal.proxy.cache.CacheProxy
import com.unde.library.internal.proxy.network.ServerProxy
import kotlinx.serialization.json.Json

internal object UndeNetworkPlugin {
    internal fun handleRequest(undeRequestResponse: UndeRequestResponse) {
        println("UndeRequest: ${undeRequestResponse.request}")
        println("UndeResponse: ${undeRequestResponse.response}")
        ServerProxy.send(WSMessage.Network(undeRequestResponse))
//        CacheProxy.cacheNetwork(undeRequestResponse)
    }
}