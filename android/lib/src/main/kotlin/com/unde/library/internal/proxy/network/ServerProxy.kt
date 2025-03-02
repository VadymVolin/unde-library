package com.unde.library.internal.proxy.network

import com.unde.library.internal.constants.DEFAULT_SERVER_HOST
import com.unde.library.internal.constants.DEFAULT_SERVER_PORT
import com.unde.library.internal.constants.DEFAULT_SERVER_WEBSOCKET_PATH
import com.unde.library.internal.proxy.network.client.HttpClientWrapper
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal object ServerProxy {

    private val client by lazy { HttpClientWrapper.get() }

    private val internalServerScope = CoroutineScope(SupervisorJob())

    private var wsSession = null

//  TODO:  receive scope from unde library class and listen active session, state flow can be used in this case
    internal fun initialize() {
//        internalServerScope.launch(Dispatchers.IO) {
//            client.webSocket(
//                method = HttpMethod.Post,
//                host = DEFAULT_SERVER_HOST,
//                port = DEFAULT_SERVER_PORT,
//                path = DEFAULT_SERVER_WEBSOCKET_PATH
//            ) {
//
//            }
//        }
    }

}
