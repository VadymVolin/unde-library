package com.unde.library.internal.proxy.cache

import android.util.Log
import com.unde.library.internal.plugin.network.model.UndeRequestResponse
import kotlinx.io.IOException
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.zip.GZIPOutputStream

internal object CacheProxy {

    private val TAG: String = CacheProxy.javaClass.simpleName

    private var networkCacheFile: File? = null
    private var databaseCacheFile: File? = null

    internal fun initialize(networkCacheFile: File? = null, databaseCacheFile: File? = null) {
        Log.d(TAG, "Initialize $TAG")
        this.networkCacheFile = networkCacheFile
        this.databaseCacheFile = databaseCacheFile
    }

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