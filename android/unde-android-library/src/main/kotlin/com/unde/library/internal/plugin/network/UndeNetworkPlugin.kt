package com.unde.library.internal.plugin.network

import android.util.Log
import com.unde.library.internal.plugin.network.model.UndeRequestResponse
import com.unde.library.internal.proxy.network.ServerSocketProxy
import com.unde.library.internal.proxy.network.model.Message

internal object UndeNetworkPlugin {
    private val TAG: String = ServerSocketProxy.javaClass.canonicalName ?: ServerSocketProxy.javaClass.simpleName
    internal fun handleRequest(undeRequestResponse: UndeRequestResponse) {
        Log.d(TAG, "UndeRequest: ${undeRequestResponse.request}")
        Log.d(TAG, "UndeResponse: ${undeRequestResponse.response}")
        ServerSocketProxy.send(Message.Network(undeRequestResponse))
//        CacheProxy.cacheNetwork(undeRequestResponse)
    }
}