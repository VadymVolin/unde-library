package com.unde.library.internal.proxy.network

import android.util.Log
import com.unde.library.internal.constants.DEFAULT_EMULATOR_SERVER_HOST
import com.unde.library.internal.constants.DEFAULT_REAL_SERVER_HOST
import com.unde.library.internal.constants.DEFAULT_RECONNECT_DELAY_MS
import com.unde.library.internal.constants.DEFAULT_SERVER_SOCKET_PORT
import com.unde.library.internal.constants.JsonTokenConstant
import com.unde.library.internal.proxy.network.model.Message
import com.unde.library.internal.utils.DeviceManager
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
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

    private val TAG: String = ServerSocketProxy.javaClass.canonicalName ?: ServerSocketProxy.javaClass.simpleName

    private enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
    }

    private val connectionState = AtomicReference(ConnectionState.DISCONNECTED)

    private var internalServerScope: CoroutineScope? = null
    private var readJob: Job? = null
    private var writeJob: Job? = null
    private var connectJob: Job? = null

    private var internalServerSelectorManager: SelectorManager? = null
    private var socket: Socket? = null
    private var readChannel: ByteReadChannel? = null
    private var writeChannel: ByteWriteChannel? = null

    private val messageQueue = ArrayDeque<Message>()

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = JsonTokenConstant.TYPE_TOKEN
    }

    internal fun initialize() {
        Log.d(TAG, "Initialize $TAG, enter")
        internalServerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        internalServerSelectorManager = SelectorManager(Dispatchers.IO)
        connect()
        Log.d(TAG, "Initialize $TAG, leave")
    }

    internal fun send(message: Message) {
        Log.d(TAG, "Send [${message.javaClass.simpleName}], enter")
        if (connectionState.get() == ConnectionState.CONNECTED) {
            Log.d(TAG, "Sending: ${message.javaClass.simpleName}")
            sendInternal(message)
        } else {
            messageQueue.add(message)
            Log.d(TAG, "Message queued (not connected): ${message.javaClass.simpleName}")
        }
        Log.d(TAG, "Send [${message.javaClass.simpleName}], leave")
    }

    internal fun destroy() {
        Log.d(TAG, "Destroy, enter")
        connectionState.set(ConnectionState.DISCONNECTED)
        readJob?.cancel()
        writeJob?.cancel()
        connectJob?.cancel()

        try {
            socket?.close()
            socket = null
            readChannel = null
            writeChannel = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket: ${e.message}", e)
        }

        internalServerScope?.cancel()
        internalServerScope = null

        internalServerSelectorManager?.close()
        internalServerSelectorManager = null

        messageQueue.clear()

        Log.d(TAG, "Destroy, leave")
    }

    private fun connect(reconnectDelay: Long? = null) {
        Log.d(TAG, "Connect (reconnect delay[$reconnectDelay]), enter")
        val scope = internalServerScope ?: return
        val selectorManager = internalServerSelectorManager ?: return

        readJob?.cancel()
        writeJob?.cancel()
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
                Log.d(TAG, "Connection established: $host:$DEFAULT_SERVER_SOCKET_PORT")
                readChannel = socket?.openReadChannel()
                writeChannel = socket?.openWriteChannel(autoFlush = true)
                startReadLoop()
                sendAllFromMessageQueue()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Connection failed: ${e.message}", e)
                connectionState.set(ConnectionState.DISCONNECTED)
                scheduleReconnect()
            }
            Log.d(TAG, "Connect (reconnect delay[$reconnectDelay]), leave")
        }
    }

    private fun startReadLoop() {
        Log.d(TAG, "startReadLoop scope[${internalServerScope != null}], readChannel[${readChannel != null}], connectionState[${connectionState.get()}], enter")
        val scope = internalServerScope ?: return
        readJob?.cancel()
        readJob = scope.launch {
            try {
                while (isActive && connectionState.get() == ConnectionState.CONNECTED) {
                    val line = readChannel?.readUTF8Line()
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
        Log.d(TAG, "startReadLoop, leave")
    }

    private fun handleMessage(line: String) = try {
        Log.d(TAG, "handleMessage, enter")
        when (val message = json.decodeFromString<Message>(line)) {
            else -> {
                Log.d(TAG, "Received message: ${message.javaClass.simpleName} - $Message")
            }
        }
        Log.d(TAG, "handleMessage, leave")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to parse incoming message: $line", e)
        Log.d(TAG, "handleMessage, leave")
    }

    private fun sendInternal(message: Message) {
        Log.d(TAG, "sendInternal [${message.javaClass.simpleName}] writeChannel[${writeChannel != null}], enter")
        val scope = internalServerScope ?: return

        if (connectionState.get() != ConnectionState.CONNECTED) {
            Log.d(TAG, "sendInternal, client is not connected, current state is ${connectionState.get()}")
            return
        }

        scope.launch {
            try {
                val encodedJsonString = json.encodeToString(message)
                val finalEncodedMessage = "${encodedJsonString.length}\n${encodedJsonString}"
                writeChannel?.writeStringUtf8(finalEncodedMessage)
                Log.d(TAG, "Sent message[${message.javaClass.simpleName}]: $finalEncodedMessage")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Failed to send message: ${e.message}", e)
                handleDisconnection()
            }
            Log.d(TAG, "sendInternal [${message.javaClass.simpleName}], leave")
        }
    }

    private fun sendAllFromMessageQueue() {
        Log.d(TAG, "sendAllFromMessageQueue messageQueue[${messageQueue.size}], connectionState[${connectionState.get()}], enter")
        while (messageQueue.isNotEmpty() && connectionState.get() == ConnectionState.CONNECTED) {
            val message = messageQueue.firstOrNull() ?: break
            sendInternal(message)
            if (connectionState.get() == ConnectionState.CONNECTED) {
                messageQueue.removeFirst()
            }
            Log.d(TAG, "sendAllFromMessageQueue: Sent queued message: ${message.javaClass.simpleName}")
        }
        Log.d(TAG, "sendAllFromMessageQueue, leave")
    }

    private fun handleDisconnection() {
        Log.d(TAG, "handleDisconnection, enter")
        if (connectionState.get() == ConnectionState.DISCONNECTED) {
            return
        }

        Log.d(TAG, "Handling disconnection")
        connectionState.set(ConnectionState.DISCONNECTED)

        readJob?.cancel()
        writeJob?.cancel()

        try {
            socket?.close()
            socket = null
            readChannel = null
            writeChannel = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket: ${e.message}", e)
        }

        scheduleReconnect()
        Log.d(TAG, "handleDisconnection, leave")
    }

    private fun scheduleReconnect() {
        Log.d(TAG, "scheduleReconnect, enter")
        connectionState.set(ConnectionState.RECONNECTING)
        Log.d(TAG, "Scheduling reconnect attempt in ${DEFAULT_RECONNECT_DELAY_MS}ms")
        connect(DEFAULT_RECONNECT_DELAY_MS)
        Log.d(TAG, "scheduleReconnect, leave")
    }
}
