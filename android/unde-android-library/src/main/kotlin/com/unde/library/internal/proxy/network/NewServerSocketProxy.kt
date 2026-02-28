package com.unde.library.internal.proxy.network

import android.R.id.message
import android.util.Log
import com.unde.library.internal.constants.DEFAULT_EMULATOR_SERVER_HOST
import com.unde.library.internal.constants.DEFAULT_MAX_FRAME_SIZE
import com.unde.library.internal.constants.DEFAULT_PING_TIMEOUT_MS
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
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.readLong
import io.ktor.utils.io.readPacket
import io.ktor.utils.io.streams.inputStream
import io.ktor.utils.io.writeLong
import io.ktor.utils.io.writePacket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.asOutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import okhttp3.internal.closeQuietly
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Internal singleton responsible for managing the TCP socket connection to the Unde server.
 *
 * This object handles connection lifecycle (connect, disconnect, reconnect),
 * message queueing (when offline), and bidirectional communication (sending/receiving [Message]).
 * It runs its own CoroutineScope IO dispatcher.
 */
//internal object NewServerSocketProxy {
//
//    // region props
//    /**
//     * Logging tag for this class.
//     */
//    private val TAG: String = NewServerSocketProxy.javaClass.canonicalName ?: NewServerSocketProxy.javaClass.simpleName
//
//    /**
//     * Scope for internal background operations.
//     */
//    private var internalServerScope: CoroutineScope? = null
//
//    /**
//     * Ktor SelectorManager for managing non-blocking I/O.
//     */
//    private var internalServerSelectorManager: SelectorManager? = null
//
//    /**
//     * The active TCP socket instance.
//     */
//    private var socket: Socket? = null
//
//    /**
//     * Channel for reading incoming data bytes.
//     */
//    private var readChannel: ByteReadChannel? = null
//
//    /**
//     * Channel for writing outgoing data bytes.
//     */
//    private var writeChannel: ByteWriteChannel? = null
//
//    /**
//     * Queue to hold messages when the client is offline/disconnected.
//     */
//    private val messageQueue = ConcurrentLinkedDeque<Message>()
//
//    /**
//     * JSON configuration for serializing/deserializing [Message] objects.
//     */
//    private val json = Json {
//        ignoreUnknownKeys = true
//        classDiscriminator = JsonTokenConstant.TYPE_TOKEN
//    }
//
//    private val connectionMutex = Mutex()
//    private val writeMutex = Mutex()
//    // endregion props
//
//    // region available api
//    /**
//     * Initializes the server socket proxy.
//     *
//     * Sets up the CoroutineScope and SelectorManager, then initiates the connection process.
//     */
//    internal fun initialize() {
//        Log.d(TAG, "Initialize $TAG, enter")
//        internalServerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
//        internalServerSelectorManager = SelectorManager(Dispatchers.IO)
//        connect()
//        Log.d(TAG, "Initialize $TAG, leave")
//    }
//
//    /**
//     * Sends a message to the connected desktop server.
//     *
//     * If connected, sends immediately. If disconnected, queues the message for later delivery.
//     *
//     * @param message The [Message] to send.
//     */
//    internal fun send(message: Message) {
//        Log.d(TAG, "Send [${message.javaClass.simpleName}], enter")
//        if (connectionState.get() == ConnectionState.CONNECTED) {
//            Log.d(TAG, "Sending: ${message.javaClass.simpleName}")
//            sendInternal(message)
//        } else {
//            messageQueue.add(message)
//            Log.d(TAG, "Message queued (not connected): ${message.javaClass.simpleName}")
//        }
//        Log.d(TAG, "Send [${message.javaClass.simpleName}], leave")
//    }
//
//    /**
//     * Destroys the proxy, closing connections and cleaning up resources.
//     *
//     * Cancels all jobs and closes the socket.
//     */
//    internal fun destroy() {
//        Log.d(TAG, "Destroy, enter")
//        try {
//            socket?.close()
//            socket = null
//            readChannel = null
//            writeChannel = null
//        } catch (e: Exception) {
//            Log.e(TAG, "Error closing socket: ${e.message}", e)
//        }
//
//        internalServerScope?.cancel()
//        internalServerScope = null
//
//        internalServerSelectorManager?.close()
//        internalServerSelectorManager = null
//
//        messageQueue.clear()
//
//        Log.d(TAG, "Destroy, leave")
//    }
//    // endregion available api
//
//    /**
//     * Initiates a connection attempt to the server.
//     *
//     * @param reconnectDelay Optional delay in milliseconds before starting the connection logic (used for backoff).
//     */
//    private fun connect(reconnectDelay: Long? = null) {
//        Log.d(TAG, "Connect (reconnect delay[$reconnectDelay]), enter")
//        val scope = internalServerScope ?: return
//        val selectorManager = internalServerSelectorManager ?: return
//        readChannel = null
//        writeChannel = null
//        socket?.closeQuietly()
//        Log.d(TAG, "Attempting to connect...")
//        scope.launch {
//            reconnectDelay?.let { delay(it) }
//            connectionMutex.withLock {
//                try {
//                    val host =
//                        if (DeviceManager.isEmulator()) DEFAULT_EMULATOR_SERVER_HOST else DEFAULT_REAL_SERVER_HOST
//                    Log.d(TAG, "Connecting to $host:$DEFAULT_SERVER_SOCKET_PORT")
//
//                    socket = aSocket(selectorManager).tcp().connect(
//                        host,
//                        DEFAULT_SERVER_SOCKET_PORT
//                    )
//                    ensureActive()
//                    Log.d(TAG, "Connection established: $host:$DEFAULT_SERVER_SOCKET_PORT")
//                    readChannel = socket?.openReadChannel()
//                    writeChannel = socket?.openWriteChannel(autoFlush = true)
//                    ensureActive()
//                    if (readChannel?.isClosedForRead == true || writeChannel?.isClosedForWrite == true || !serverHandshake()) {
//                        error("Cannot connect to remote server, actual server is down")
//                    }
//                    ensureActive()
//                    startReadLoop()
//                    sendAllFromMessageQueue()
//                } catch (e: Exception) {
//                    if (e is CancellationException) throw e
//                    Log.e(TAG, "Connection failed: ${e.message}", e)
//                    scheduleReconnect()
//                }
//            }
//            Log.d(TAG, "Connect (reconnect delay[$reconnectDelay]), leave")
//        }
//    }
//
//    @Throws(IllegalStateException::class)
//    private suspend fun serverHandshake(): Boolean {
//        Log.d(TAG, "serverHandshake scope[${internalServerScope != null}], readChannel[${readChannel != null}], connectionState[${connectionState.get()}], enter")
//        var handshakeResult = false
//        val scope = internalServerScope ?: return false
//        if (connectionState.get() != ConnectionState.CONNECTING) {
//            Log.d(TAG, "serverHandshake, client is not connecting, current state is ${connectionState.get()}")
//            handshakeResult = false
//        } else {
//            val asyncPingResult = scope.async {
//                Log.d(TAG, "serverHandshake: start reading handshake response asynchronously")
//                withTimeoutOrNull(DEFAULT_PING_TIMEOUT_MS) {
//                    runCatching {
//                        readChannel?.readFramedJsonSafe(true)
//                    }.onFailure {
//                        Log.w(TAG, "serverHandshake: cannot read ping result", it)
//                    }.getOrNull()
//                }
//            }
//            val message = Message.Plain("Ping")
//            writeChannel?.writeFramedJsonSafe(message, true)
//            Log.d(TAG, "serverHandshake => Sent message[${message.javaClass.simpleName}]")
//            asyncPingResult.await()?.let {
//                handleMessage(it)
//                if (it is Message.Plain) {
//                    handshakeResult = it.data == "Pong"
//                }
//            } ?: { handshakeResult = false }
//        }
//        return handshakeResult.also {
//            Log.d(TAG, "serverHandshake [${message.javaClass.simpleName}], result [$it], leave")
//        }
//    }
//
//    /**
//     * Starts a coroutine loop to read incoming messages from the [readChannel].
//     *
//     * Continually reads lines from the socket until disconnected or canceled.
//     */
//    private fun startReadLoop() {
//        Log.d(TAG, "startReadLoop scope[${internalServerScope != null}], readChannel[${readChannel != null}], connectionState[${connectionState.get()}], enter")
//        val scope = internalServerScope ?: return
//        readJob?.cancel()
//        readJob = scope.launch {
//            try {
//                while (isActive && connectionState.get() == ConnectionState.CONNECTED) {
//                    handleMessage(runCatching {
//                        readChannel?.readFramedJsonSafe(true)
//                    }.onFailure {
//                        Log.w(TAG, "startReadLoop: cannot read server message", it)
//                    }.getOrNull())
//                }
//            } catch (e: Exception) {
//                if (e is CancellationException) throw e
//                Log.e(TAG, "Read loop error: ${e.message}", e)
//            } finally {
//                Log.w(TAG, "Read loop ended")
//                handleDisconnection()
//            }
//        }
//        Log.d(TAG, "startReadLoop, leave")
//    }
//
//    /**
//     * Parses a raw line of text into a [Message] object.
//     *
//     * @param message Raw JSON string received from the socket.
//     */
//    private fun handleMessage(message: Message?) = try {
//        Log.d(TAG, "handleMessage, enter")
//        when (message) {
//            else -> {
//                Log.d(TAG, "Received message: ${message?.javaClass?.simpleName} - $message")
//            }
//        }
//        Log.d(TAG, "handleMessage, leave")
//    } catch (e: Exception) {
//        Log.e(TAG, "Failed to parse incoming message: $message", e)
//        Log.d(TAG, "handleMessage, leave")
//    }
//
//    /**
//     * Writes a message to the [writeChannel].
//     *
//     * Serializes the message to JSON, prefixes it with length, and writes it to the socket.
//     *
//     * @param message The [Message] to write.
//     */
//    private fun sendInternal(message: Message) {
//        Log.d(TAG, "sendInternal [${message.javaClass.simpleName}] writeChannel[${writeChannel != null}], enter")
//        val scope = internalServerScope ?: return
//
//        if (connectionState.get() != ConnectionState.CONNECTED) {
//            Log.d(TAG, "sendInternal, client is not connected, current state is ${connectionState.get()}")
//            return
//        }
//
//        scope.launch {
//            try {
//                writeChannel?.writeFramedJsonSafe(message, true)
//                Log.d(TAG, "Sent message[${message.javaClass.simpleName}]")
//            } catch (e: Exception) {
//                if (e is CancellationException) throw e
//                Log.e(TAG, "Failed to send message: ${e.message}", e)
//                handleDisconnection()
//            }
//            Log.d(TAG, "sendInternal [${message.javaClass.simpleName}], leave")
//        }
//    }
//
//    /**
//     * Flushes the [messageQueue], trying to send all pending messages.
//     *
//     * Stops if the connection drops during processing.
//     */
//    private fun sendAllFromMessageQueue() {
//        Log.d(TAG, "sendAllFromMessageQueue messageQueue[${messageQueue.size}], connectionState[${connectionState.get()}], enter")
//        while (messageQueue.isNotEmpty() && connectionState.get() == ConnectionState.CONNECTED) {
//            val message = messageQueue.firstOrNull() ?: break
//            sendInternal(message)
//            if (connectionState.get() == ConnectionState.CONNECTED) {
//                messageQueue.removeFirst()
//            }
//            Log.d(TAG, "sendAllFromMessageQueue: Sent queued message: ${message.javaClass.simpleName}")
//        }
//        Log.d(TAG, "sendAllFromMessageQueue, leave")
//    }
//
//    /**
//     * Handles cleanup after a socket disconnection event.
//     *
//     * Closes resources and triggers a reconnection schedule.
//     */
//    private fun handleDisconnection() {
//        Log.d(TAG, "handleDisconnection, enter")
//        try {
//            readChannel = null
//            writeChannel = null
//            socket?.close()
//        } catch (e: Exception) {
//            Log.e(TAG, "Error closing socket: ${e.message}", e)
//            socket?.closeQuietly()
//        } finally {
//            socket = null
//        }
//
//        scheduleReconnect()
//        Log.d(TAG, "handleDisconnection, leave")
//    }
//
//    /**
//     * Schedules a delayed call to [connect] to attempt reconnection.
//     */
//    private fun scheduleReconnect() {
//        Log.d(TAG, "scheduleReconnect, enter")
//        Log.d(TAG, "Scheduling reconnect attempt in ${DEFAULT_RECONNECT_DELAY_MS}ms")
//        connect(DEFAULT_RECONNECT_DELAY_MS)
//        Log.d(TAG, "scheduleReconnect, leave")
//    }
//
//    // region read-write channels extensions
//    @OptIn(ExperimentalSerializationApi::class)
//    suspend fun ByteWriteChannel.writeFramedJsonSafe(data: Message, compress: Boolean) {
//        val packet = buildPacket {
//            currentCoroutineContext().ensureActive()
//            if (compress) {
//                GZIPOutputStream(asOutputStream()).use {
//                    json.encodeToStream(Message.serializer(), data, it)
//                    it.finish()
//                }
//            } else {
//                asOutputStream().use {
//                    json.encodeToStream(Message.serializer(), data, it)
//                }
//            }
//        }
//        val size = packet.remaining
//        currentCoroutineContext().ensureActive()
//        require(size >= 0) { "Negative frame size" }
//        writeLong(size)
//        writePacket(packet)
//        flush()
//    }
//
//    @OptIn(ExperimentalSerializationApi::class)
//    suspend fun ByteReadChannel.readFramedJsonSafe(
//        decompress: Boolean,
//        maxFrameSize: Long = DEFAULT_MAX_FRAME_SIZE // 50MB safety limit
//    ): Message {
//        val size = readLong()
//        if (size !in 1..maxFrameSize) {
//            throw IllegalStateException("Invalid frame size: $size")
//        }
//        val packet = readPacket(size.toInt())
//        currentCoroutineContext().ensureActive()
//        val stream = if (decompress) {
//                GZIPInputStream(packet.inputStream())
//        } else {
//            packet.inputStream()
//        }
//        currentCoroutineContext().ensureActive()
//        return try {
//            stream.use {
//                json.decodeFromStream(Message.serializer(), it)
//            }
//        } catch (e: SerializationException) {
//            throw IllegalStateException("Corrupted JSON frame", e)
//        }
//    }
//    // endregion
//}
