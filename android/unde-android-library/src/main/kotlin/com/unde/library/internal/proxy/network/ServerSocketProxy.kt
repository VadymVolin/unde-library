package com.unde.library.internal.proxy.network

import android.util.Log
import com.unde.library.internal.constants.DEFAULT_EMULATOR_SERVER_HOST
import com.unde.library.internal.constants.DEFAULT_KEEP_ALIVE_INTERVAL_MS
import com.unde.library.internal.constants.DEFAULT_KEEP_ALIVE_TIMEOUT_MS
import com.unde.library.internal.constants.DEFAULT_REAL_SERVER_HOST
import com.unde.library.internal.constants.DEFAULT_RECONNECT_DELAY_MS
import com.unde.library.internal.constants.DEFAULT_SERVER_SOCKET_PORT
import com.unde.library.internal.constants.JsonTokenConstant
import com.unde.library.internal.proxy.network.model.Message
import com.unde.library.internal.utils.DeviceManager
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicReference

internal object ServerSocketProxy {

    private val TAG: String = ServerSocketProxy.javaClass.simpleName

    private enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
    }

    private val connectionState = AtomicReference(ConnectionState.DISCONNECTED)

    private var internalServerScope: CoroutineScope? = null
    private var readJob: Job? = null
    private var writeJob: Job? = null
    private var keepAliveJob: Job? = null
    private var connectJob: Job? = null

    private var internalServerSelectorManager: SelectorManager? = null
    private var socket: Socket? = null

    private val messageQueue = ArrayDeque<Message>()

    @Volatile
    private var lastKeepAliveTime = 0L

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = JsonTokenConstant.TYPE_TOKEN
    }

    internal fun initialize() {
        Log.d(TAG, "Initialize $TAG")
        internalServerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        internalServerSelectorManager = SelectorManager(Dispatchers.IO)
        connect()
    }

    internal fun send(message: Message) {
        if (connectionState.get() == ConnectionState.CONNECTED) {
            Log.d(TAG, "Sending: ${message.javaClass.simpleName}")
            sendInternal(message)
        } else {
            messageQueue.add(message)
            Log.d(TAG, "Message queued (not connected): ${message.javaClass.simpleName}")
        }
    }

    internal fun destroy() {
        Log.d(TAG, "Destroy $TAG")
        connectionState.set(ConnectionState.DISCONNECTED)
        readJob?.cancel()
        writeJob?.cancel()
        keepAliveJob?.cancel()
        connectJob?.cancel()

        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket: ${e.message}", e)
        }

        internalServerScope?.cancel()
        internalServerScope = null

        internalServerSelectorManager?.close()
        internalServerSelectorManager = null

        messageQueue.clear()

        Log.d(TAG, "$TAG destroyed")
    }

    private fun connect(reconnectDelay: Long? = null) {
        val scope = internalServerScope ?: return
        val selectorManager = internalServerSelectorManager ?: return

        readJob?.cancel()
        writeJob?.cancel()
        keepAliveJob?.cancel()
        connectJob?.cancel()

        if (connectionState.get() == ConnectionState.CONNECTING || connectionState.get() == ConnectionState.CONNECTED) {
            Log.d(TAG, "Already connecting or connected, skipping connect attempt")
            return
        }

        connectionState.set(ConnectionState.CONNECTING)
        Log.d(TAG, "Attempting to connect...")

        connectJob = scope.launch {
            reconnectDelay?.let { delay(it) }
            try {
                val host = if (DeviceManager.isEmulator()) DEFAULT_EMULATOR_SERVER_HOST else DEFAULT_REAL_SERVER_HOST
                Log.d(TAG, "Connecting to $host:$DEFAULT_SERVER_SOCKET_PORT")

                socket = aSocket(selectorManager).tcp().connect(
                    host,
                    DEFAULT_SERVER_SOCKET_PORT
                )
                
                connectionState.set(ConnectionState.CONNECTED)
                lastKeepAliveTime = System.currentTimeMillis()
                Log.d(TAG, "Connection established: $host:$DEFAULT_SERVER_SOCKET_PORT")
                startReadLoop()
                startKeepAlive()
                sendAllFromMessageQueue()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Connection failed: ${e.message}", e)
                connectionState.set(ConnectionState.DISCONNECTED)
                scheduleReconnect()
            }
        }
    }

    private fun startReadLoop() {
        val scope = internalServerScope ?: return
        val currentSocket = socket ?: return
        readJob?.cancel()
        readJob = scope.launch {
            try {
                val receiveChannel = currentSocket.openReadChannel()
                while (isActive && connectionState.get() == ConnectionState.CONNECTED) {
                    val line = receiveChannel.readUTF8Line()
                    ensureActive()
                    if (line == null) {
                        Log.w(TAG, "Received null line, connection closed by server")
                        break
                    }
                    if (line.isNotBlank()) {
                        handleMessage(line)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Read loop error: ${e.message}", e)
            } finally {
                Log.w(TAG, "Read loop ended")
                handleDisconnection()
            }
        }
    }

    private fun handleMessage(line: String) = try {
        when (val message = json.decodeFromString<Message>(line)) {
            is Message.KeepAlive -> {
                lastKeepAliveTime = System.currentTimeMillis()
                Log.d(TAG, "Received keep-alive message")
            }
            else -> {
                Log.d(TAG, "Received message: ${message.javaClass.simpleName}")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to parse incoming message: $line", e)
    }

    private fun startKeepAlive() {
        val scope = internalServerScope ?: return
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive && connectionState.get() == ConnectionState.CONNECTED) {
                try {
                    delay(DEFAULT_KEEP_ALIVE_INTERVAL_MS)
                    ensureActive()

                    val timeSinceLastKeepAlive = System.currentTimeMillis() - lastKeepAliveTime
                    if (timeSinceLastKeepAlive > DEFAULT_KEEP_ALIVE_TIMEOUT_MS) {
                        Log.w(TAG, "Keep-alive timeout (${timeSinceLastKeepAlive}ms), connection lost")
                        handleDisconnection()
                        break
                    }
                    sendInternal(Message.KeepAlive())
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Keep-alive error: ${e.message}", e)
                }
            }
        }
    }

    private fun sendInternal(message: Message) {
        val scope = internalServerScope ?: return
        val currentSocket = socket ?: return
        
        if (connectionState.get() != ConnectionState.CONNECTED) {
            return
        }

        scope.launch {
            try {
                val writeChannel = currentSocket.openWriteChannel(autoFlush = true)
                val encodedJson = json.encodeToString(message)
                writeChannel.writeStringUtf8("$encodedJson\n")
                if (message !is Message.KeepAlive) {
                    Log.d(TAG, "Sent message[${message.javaClass.simpleName}]: $encodedJson")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Failed to send message: ${e.message}", e)
                handleDisconnection()
            }
        }
    }

    private fun sendAllFromMessageQueue() = internalServerScope?.launch {
        while (messageQueue.isNotEmpty() && connectionState.get() == ConnectionState.CONNECTED) {
            val message = messageQueue.firstOrNull() ?: break
            sendInternal(message)
            if (connectionState.get() == ConnectionState.CONNECTED) {
                messageQueue.removeFirst()
            }
            Log.d(TAG, "sendAllFromMessageQueue: Sent queued message: ${message.javaClass.simpleName}")
        }
    }

    private fun handleDisconnection() {
        if (connectionState.get() == ConnectionState.DISCONNECTED) {
            return
        }

        Log.d(TAG, "Handling disconnection")
        connectionState.set(ConnectionState.DISCONNECTED)
        
        readJob?.cancel()
        writeJob?.cancel()
        keepAliveJob?.cancel()
        
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket: ${e.message}", e)
        }

        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        connectionState.set(ConnectionState.RECONNECTING)
        Log.d(TAG, "Scheduling reconnect attempt in ${DEFAULT_RECONNECT_DELAY_MS}ms")
        connect(DEFAULT_RECONNECT_DELAY_MS)
    }
}
