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

/**
 * Internal singleton responsible for managing the TCP socket connection to the Unde server.
 *
 * This object handles connection lifecycle (connect, disconnect, reconnect),
 * message queueing (when offline), and bidirectional communication (sending/receiving [Message]).
 * It runs its own CoroutineScope IO dispatcher.
 */
internal object ServerSocketProxy {

    /**
     * Logging tag for this class.
     */
    private val TAG: String = ServerSocketProxy.javaClass.canonicalName ?: ServerSocketProxy.javaClass.simpleName

    /**
     * Enumeration of possible socket connection states.
     */
    private enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
    }

    /**
     * Thread-safe reference to the current [ConnectionState].
     */
    private val connectionState = AtomicReference(ConnectionState.DISCONNECTED)

    /**
     * Scope for internal background operations.
     */
    private var internalServerScope: CoroutineScope? = null

    /**
     * Job specifically for reading from the socket.
     */
    private var readJob: Job? = null

    /**
     * Job specifically for writing to the socket (if using a dedicated job).
     */
    private var writeJob: Job? = null

    /**
     * Job managing the connection attempt sequence.
     */
    private var connectJob: Job? = null

    /**
     * Ktor SelectorManager for managing non-blocking I/O.
     */
    private var internalServerSelectorManager: SelectorManager? = null

    /**
     * The active TCP socket instance.
     */
    private var socket: Socket? = null

    /**
     * Channel for reading incoming data bytes.
     */
    private var readChannel: ByteReadChannel? = null

    /**
     * Channel for writing outgoing data bytes.
     */
    private var writeChannel: ByteWriteChannel? = null

    /**
     * Queue to hold messages when the client is offline/disconnected.
     */
    private val messageQueue = ArrayDeque<Message>()

    /**
     * JSON configuration for serializing/deserializing [Message] objects.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = JsonTokenConstant.TYPE_TOKEN
    }

    /**
     * Initializes the server socket proxy.
     *
     * Sets up the CoroutineScope and SelectorManager, then initiates the connection process.
     */
    internal fun initialize() {
        Log.d(TAG, "Initialize $TAG, enter")
        internalServerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        internalServerSelectorManager = SelectorManager(Dispatchers.IO)
        connect()
        Log.d(TAG, "Initialize $TAG, leave")
    }

    /**
     * Sends a message to the connected desktop server.
     *
     * If connected, sends immediately. If disconnected, queues the message for later delivery.
     *
     * @param message The [Message] to send.
     */
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

    /**
     * Destroys the proxy, closing connections and cleaning up resources.
     *
     * Cancels all jobs and closes the socket.
     */
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

    /**
     * Initiates a connection attempt to the server.
     *
     * @param reconnectDelay Optional delay in milliseconds before starting the connection logic (used for backoff).
     */
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

    /**
     * Starts a coroutine loop to read incoming messages from the [readChannel].
     *
     * Continually reads lines from the socket until disconnected or cancelled.
     */
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

    /**
     * Parses a raw line of text into a [Message] object.
     *
     * @param line Raw JSON string received from the socket.
     */
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

    /**
     * Writes a message to the [writeChannel].
     *
     * Serializes the message to JSON, prefixes it with length, and writes it to the socket.
     *
     * @param message The [Message] to write.
     */
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

    /**
     * Flushes the [messageQueue], trying to send all pending messages.
     *
     * Stops if the connection drops during processing.
     */
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

    /**
     * Handles cleanup after a socket disconnection event.
     *
     * Closes resources and triggers a reconnection schedule.
     */
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

    /**
     * Schedules a delayed call to [connect] to attempt reconnection.
     */
    private fun scheduleReconnect() {
        Log.d(TAG, "scheduleReconnect, enter")
        connectionState.set(ConnectionState.RECONNECTING)
        Log.d(TAG, "Scheduling reconnect attempt in ${DEFAULT_RECONNECT_DELAY_MS}ms")
        connect(DEFAULT_RECONNECT_DELAY_MS)
        Log.d(TAG, "scheduleReconnect, leave")
    }
}
