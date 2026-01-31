package com.unde.library

import com.unde.library.internal.proxy.cache.CacheProxy
import com.unde.library.internal.proxy.network.ServerSocketProxy
import java.io.File

/**
 * Entry point for the Unde Android Library.
 *
 * This class is responsible for initializing and destroying the library's core components,
 * including the network proxy and cache mechanisms.
 */
class UndeLibrary {
    /**
     * Initializes the library with optional cache file locations.
     *
     * This method sets up the [CacheProxy] and starts the [ServerSocketProxy].
     * It should be called once during the application's lifecycle, typically in the Application class.
     *
     * @param networkCacheFile Optional [File] to be used for storing network cache. If null, a default location or in-memory cache might be used depending on implementation.
     * @param databaseCacheFile Optional [File] to be used for storing database cache.
     */
    fun initialize(networkCacheFile: File? = null, databaseCacheFile: File? = null) {
        CacheProxy.initialize(networkCacheFile, databaseCacheFile)
        ServerSocketProxy.initialize()
    }

    /**
     * Destroys the library resources.
     *
     * This method stops the [ServerSocketProxy] and cleans up any active resources.
     * It should be called when the library is no longer needed, though typically the library lives as long as the application.
     */
    fun destroy() {
        ServerSocketProxy.destroy()
    }
}