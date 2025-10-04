package com.unde.library.internal.proxy.network

import android.util.Log
import com.unde.library.internal.constants.DEFAULT_EMULATOR_SERVER_HOST
import com.unde.library.internal.constants.DEFAULT_REAL_SERVER_HOST
import com.unde.library.internal.constants.DEFAULT_SERVER_WEBSOCKET_PATH
import com.unde.library.internal.constants.DEFAULT_SERVER_WS_PORT
import com.unde.library.internal.plugin.network.model.WSMessage
import com.unde.library.internal.proxy.network.client.HttpClientWrapper
import com.unde.library.internal.utils.DeviceManager
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

internal object ServerProxy {

    private val TAG: String = ServerProxy.javaClass.simpleName

    private val client by lazy { HttpClientWrapper.get() }
    private var internalServerScope: CoroutineScope? = null
    private var wsSession: DefaultClientWebSocketSession? = null
        set(value) {
            Log.d(TAG, "SET WEB SOCKET SESSION: ")
            field = value
        }

    internal fun initialize() {
        Log.d(TAG, "Initialize $TAG")
        internalServerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        internalServerScope?.let { scope ->
            flow {
                client.webSocketSession(
                    method = HttpMethod.Get,
                    host = if (DeviceManager.isEmulator()) DEFAULT_EMULATOR_SERVER_HOST else DEFAULT_REAL_SERVER_HOST,
                    port = DEFAULT_SERVER_WS_PORT,
                    path = DEFAULT_SERVER_WEBSOCKET_PATH
                ).also { flow ->
                    wsSession = flow
                    Log.d(TAG, "Connection established: ${if (DeviceManager.isEmulator()) DEFAULT_EMULATOR_SERVER_HOST else DEFAULT_REAL_SERVER_HOST}")
                    emitAll(channel = flow.incoming)
                }
            }.onEach {
                Log.d(TAG, "Message has been received: $it")
            }.catch {

            }.flowOn(Dispatchers.IO).launchIn(scope)
        }
    }

    internal fun send(wsMessage: WSMessage) {
        internalServerScope?.launch {
            ensureActive()
            wsSession?.send(Frame.Text(Json.encodeToString(wsMessage)))?.also {
                Log.d(TAG, "Send message: ${wsMessage.javaClass.name}")
            }
        } ?: Log.d(TAG, "Cannot send message, scope is canceled!")
    }

    internal fun destroy() = internalServerScope?.launch {
        Log.d(TAG, "Destroy $TAG")
        ensureActive()
        wsSession?.close()
        wsSession = null
        cancel()
    } ?: Log.d(TAG, "Cannot close ws session, scope is canceled or null!")
}
