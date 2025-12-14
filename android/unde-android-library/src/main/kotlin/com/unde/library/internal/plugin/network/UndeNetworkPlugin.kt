package com.unde.library.internal.plugin.network

import android.util.Log
import com.unde.library.internal.plugin.network.model.UndeRequestResponse
import com.unde.library.internal.proxy.network.model.WSMessage
import com.unde.library.internal.proxy.network.ServerProxy

internal object UndeNetworkPlugin {
    private val TAG: String = UndeNetworkPlugin.javaClass.simpleName
    internal fun handleRequest(undeRequestResponse: UndeRequestResponse) {
        Log.d(TAG, "UndeRequest: ${undeRequestResponse.request}")
        Log.d(TAG, "UndeResponse: ${undeRequestResponse.response}")
        ServerProxy.send(WSMessage.Network(undeRequestResponse))
//        CacheProxy.cacheNetwork(undeRequestResponse)
    }
}