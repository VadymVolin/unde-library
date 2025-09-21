package com.unde.library.internal.proxy.network

import android.util.Log
import com.unde.library.internal.constants.DEFAULT_EMULATOR_SERVER_HOST
import com.unde.library.internal.constants.DEFAULT_REAL_SERVER_HOST
import com.unde.library.internal.constants.DEFAULT_SERVER_WEBSOCKET_PATH
import com.unde.library.internal.constants.DEFAULT_SERVER_WS_PORT
import com.unde.library.internal.plugin.network.model.WSMessage
import com.unde.library.internal.proxy.cache.CacheProxy
import com.unde.library.internal.proxy.network.client.HttpClientWrapper
import com.unde.library.internal.utils.DeviceManager
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.util.reflect.TypeInfo
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

internal object ServerProxy {

    private val TAG: String = ServerProxy.javaClass.simpleName

    private val client by lazy { HttpClientWrapper.get() }
    private var internalServerScope: CoroutineScope? = null
    private var wsSessionJob: Job? = null
    private var wsFlow: Flow<Frame>? = null
    private var wsSession: DefaultClientWebSocketSession? = null

    internal fun initialize() {
        println(TAG + " Initialize $TAG")
//        Log.d(TAG, "Initialize $TAG")
        internalServerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        wsFlow = flow {
            wsSession = client.webSocketSession(
                method = HttpMethod.Post,
                host = if (DeviceManager.isEmulator()) DEFAULT_EMULATOR_SERVER_HOST else DEFAULT_REAL_SERVER_HOST,
                port = DEFAULT_SERVER_WS_PORT,
                path = DEFAULT_SERVER_WEBSOCKET_PATH
            ).also {
                emitAll(channel = it.incoming)
            }
        }
        wsSessionJob = internalServerScope?.launch(Dispatchers.IO) {
            ensureActive()
            wsFlow?.collect {
                ensureActive()
                println(TAG + " Message has been received: $it")
            }
        }
    }

    internal fun send(wsMessage: WSMessage) {
//        Log.d(TAG, "send: ${wsMessage.javaClass.name}")
        println(TAG + " send: ${(wsMessage as? WSMessage.Network)?.data?.response?.body}")
        println(TAG + " send: ${Json.encodeToString(wsMessage)}")
        internalServerScope?.launch {
            ensureActive()
            wsSession?.send(Frame.Text(Json.encodeToString(wsMessage)))
        }
//        } ?: Log.d(TAG, "Cannot send message, scope is canceled!")
    }

    internal fun destroy() = internalServerScope?.launch {
//        Log.d(TAG, "Destroy $TAG")
        println(TAG + " Destroy $TAG")
        ensureActive()
        wsSessionJob?.cancel()
        wsSession?.close()
        wsSession = null
        cancel()
    }
//    } ?: Log.d(TAG, "Cannot close ws session, scope is canceled or null!")
}
