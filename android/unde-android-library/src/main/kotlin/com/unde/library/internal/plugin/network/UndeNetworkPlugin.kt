package com.unde.library.internal.plugin.network

import com.unde.library.internal.plugin.network.model.UndeRequestResponse
import com.unde.library.internal.proxy.cache.CacheProxy

internal object UndeNetworkPlugin {
    internal fun handleRequest(undeRequestResponse: UndeRequestResponse) {
        println("UndeRequest: ${undeRequestResponse.request}")
        println("UndeResponse: ${undeRequestResponse.response}")
//        CacheProxy.cacheNetwork(undeRequestResponse)
    }
}