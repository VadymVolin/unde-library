package com.unde.library.internal.proxy.cache

import android.util.Log
import com.unde.library.internal.plugin.network.model.UndeRequestResponse
import com.unde.library.internal.proxy.network.ServerSocketProxy
import kotlinx.io.IOException
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.zip.GZIPOutputStream

/**
 * Internal singleton responsible for caching intercepted data to the local file system.
 *
 * Currently supports caching network responses using GZIP compression.
 * This ensures data persists even if the immediate socket connection is lost or for later inspection.
 */
internal object CacheProxy {

    /**
     * Logging tag.
     */
    private val TAG: String = ServerSocketProxy.javaClass.canonicalName ?: ServerSocketProxy.javaClass.simpleName

    /**
     * Target file/directory for storing network cache.
     */
    private var networkCacheFile: File? = null

    /**
     * Target file/directory for storing database cache.
     */
    private var databaseCacheFile: File? = null

    /**
     * Initializes the cache proxy with storage locations.
     *
     * @param networkCacheFile Directory/File for network cache.
     * @param databaseCacheFile Directory/File for database cache.
     */
    internal fun initialize(networkCacheFile: File? = null, databaseCacheFile: File? = null) {
        Log.d(TAG, "Initialize $TAG")
        this.networkCacheFile = networkCacheFile
        this.databaseCacheFile = databaseCacheFile
    }

    /**
     * Caches a network request/response pair to disk.
     *
     * Compresses the data using GZIP before writing to the configured [networkCacheFile].
     *
     * @param requestResponse The [UndeRequestResponse] to cache.
     */
    internal suspend fun cacheNetwork(requestResponse: UndeRequestResponse) {
        try {
            networkCacheFile?.let {
                FileOutputStream(it).use { fos ->
                    GZIPOutputStream(fos).use { gzipOut ->
                        gzipOut.write(requestResponse.toString().toByteArray(Charset.defaultCharset()))
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace() // Handle file writing/compression errors
        }
    }
}