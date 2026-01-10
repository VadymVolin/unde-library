package com.unde.library

import com.unde.library.internal.proxy.cache.CacheProxy
import com.unde.library.internal.proxy.network.ServerSocketProxy
import com.unde.library.internal.proxy.network.ServerWebSocketProxy
import java.io.File

class UndeLibrary {
    fun initialize(networkCacheFile: File? = null, databaseCacheFile: File? = null) {
        CacheProxy.initialize(networkCacheFile, databaseCacheFile)
        ServerWebSocketProxy.initialize()
        ServerSocketProxy.initialize()
    }

    fun destroy() {
        ServerWebSocketProxy.destroy()
        ServerSocketProxy.destroy()
    }
}