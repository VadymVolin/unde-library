package com.unde.library.internal.plugin.network

import android.util.Log
import com.unde.library.internal.plugin.network.model.UndeRequestResponse
import com.unde.library.internal.proxy.network.ServerSocketProxy
import com.unde.library.internal.proxy.network.model.Message

/**
 * Internal plugin entry point for handing network interception logic.
 *
 * Receives [UndeRequestResponse] objects from interceptors and routes them
 * to the appropriate destinations (Logging, Socket Proxy, Cache Proxy).
 */
internal object UndeNetworkPlugin {
    /**
     * Logging tag.
     */
    private val TAG: String = UndeNetworkPlugin.javaClass.canonicalName ?: UndeNetworkPlugin.javaClass.simpleName
    /**
     * Processes an intercepted network request and response.
     *
     * Logs the event and forwards it to the [ServerSocketProxy] to be sent to the desktop tool.
     * Can also trigger [com.unde.library.internal.proxy.cache.CacheProxy] (currently commented out).
     *
     * @param undeRequestResponse The combined request and response data.
     */
    internal fun handleRequest(undeRequestResponse: UndeRequestResponse) {
        Log.d(TAG, "Received new request-response")
        ServerSocketProxy.send(Message.Network(undeRequestResponse))
//        CacheProxy.cacheNetwork(undeRequestResponse)
    }
}