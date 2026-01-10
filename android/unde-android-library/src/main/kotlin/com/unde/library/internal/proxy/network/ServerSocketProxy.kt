package com.unde.library.internal.proxy.network

import android.util.Log
import com.unde.library.internal.constants.DEFAULT_EMULATOR_SERVER_HOST
import com.unde.library.internal.constants.DEFAULT_KEEP_ALIVE_INTERVAL_MS
import com.unde.library.internal.constants.DEFAULT_KEEP_ALIVE_TIMEOUT_MS
import com.unde.library.internal.constants.DEFAULT_MAX_RECONNECT_ATTEMPTS
import com.unde.library.internal.constants.DEFAULT_REAL_SERVER_HOST
import com.unde.library.internal.constants.DEFAULT_RECONNECT_DELAY_MS
import com.unde.library.internal.constants.DEFAULT_SERVER_SOCKET_PORT
import com.unde.library.internal.proxy.network.model.WSMessage
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min
import kotlin.math.pow

internal object ServerSocketProxy {

    private val TAG: String = ServerSocketProxy.javaClass.simpleName

    // Connection state
    private enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
    }

    private val connectionState = AtomicReference(ConnectionState.DISCONNECTED)

    // Coroutine scope and jobs
    private var internalServerScope: CoroutineScope? = null
    private var readJob: Job? = null
    private var writeJob: Job? = null
    private var keepAliveJob: Job? = null
    private var reconnectJob: Job? = null

    // Socket and selector
    private var internalServerSelectorManager: SelectorManager? = null
    private var socket: Socket? = null

    // Thread-safe message queue for offline messages
    private val messageQueue = ConcurrentLinkedQueue<WSMessage>()

    // Keep-alive tracking
    @Volatile
    private var lastKeepAliveTime = 0L

    // Reconnection tracking
    @Volatile
    private var reconnectAttempts = 0

    // JSON serializer
    private val json = Json { ignoreUnknownKeys = true }

    internal fun initialize() {
        Log.d(TAG, "Initialize $TAG")
        internalServerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        internalServerSelectorManager = SelectorManager(Dispatchers.IO)
        connect()
    }

    private fun connect() {
        val scope = internalServerScope ?: return
        val selectorManager = internalServerSelectorManager ?: return

        if (connectionState.get() == ConnectionState.CONNECTING || 
            connectionState.get() == ConnectionState.CONNECTED) {
            Log.d(TAG, "Already connecting or connected, skipping connect attempt")
            return
        }

        connectionState.set(ConnectionState.CONNECTING)
        Log.d(TAG, "Attempting to connect...")

        scope.launch {
            try {
                val host = if (DeviceManager.isEmulator()) DEFAULT_EMULATOR_SERVER_HOST else DEFAULT_REAL_SERVER_HOST
                Log.d(TAG, "Connecting to $host:$DEFAULT_SERVER_SOCKET_PORT")

                socket = aSocket(selectorManager).tcp().connect(host, DEFAULT_SERVER_SOCKET_PORT)
                
                connectionState.set(ConnectionState.CONNECTED)
                reconnectAttempts = 0
                lastKeepAliveTime = System.currentTimeMillis()
                
                Log.d(TAG, "Connection established: $host:$DEFAULT_SERVER_SOCKET_PORT")

                // Start reading and writing
                startReadLoop()
                startKeepAlive()
                
                // Send queued messages
                flushMessageQueue()

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
                    ensureActive()
                    
                    val line = receiveChannel.readUTF8Line()
                    
                    if (line == null) {
                        Log.w(TAG, "Received null line, connection closed by server")
                        break
                    }
                    
                    if (line.isNotEmpty()) {
                        handleIncomingMessage(line)
                    }
                }
                
                Log.w(TAG, "Read loop ended")
                handleDisconnection()
                
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Read loop error: ${e.message}", e)
                handleDisconnection()
            }
        }
    }

    private fun handleIncomingMessage(line: String) {
        try {
            val message = json.decodeFromString<WSMessage>(line)
            
            when (message) {
                is WSMessage.KeepAlive -> {
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
    }

    private fun startKeepAlive() {
        val scope = internalServerScope ?: return

        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive && connectionState.get() == ConnectionState.CONNECTED) {
                try {
                    delay(DEFAULT_KEEP_ALIVE_INTERVAL_MS)
                    ensureActive()

                    // Check if we received a keep-alive recently
                    val timeSinceLastKeepAlive = System.currentTimeMillis() - lastKeepAliveTime
                    if (timeSinceLastKeepAlive > DEFAULT_KEEP_ALIVE_TIMEOUT_MS) {
                        Log.w(TAG, "Keep-alive timeout (${timeSinceLastKeepAlive}ms), connection lost")
                        handleDisconnection()
                        break
                    }

                    // Send keep-alive
                    sendMessageInternal(WSMessage.KeepAlive())

                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Keep-alive error: ${e.message}", e)
                }
            }
        }
    }

    private fun sendMessageInternal(message: WSMessage): Boolean {
        val currentSocket = socket ?: return false
        
        if (connectionState.get() != ConnectionState.CONNECTED) {
            return false
        }

        return try {
            val scope = internalServerScope ?: return false
            
            scope.launch {
                try {
                    val writeChannel = currentSocket.openWriteChannel(autoFlush = true)
                    val encodedJson = json.encodeToString(message)
                    writeChannel.writeStringUtf8("$encodedJson\n")
                    
                    if (message !is WSMessage.KeepAlive) {
                        Log.d(TAG, "Sent message[${message.javaClass.simpleName}]: $encodedJson")
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Failed to send message: ${e.message}", e)
                    handleDisconnection()
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message: ${e.message}", e)
            false
        }
    }

    internal fun send(wsMessage: WSMessage) {
        if (connectionState.get() == ConnectionState.CONNECTED) {
            sendMessageInternal(wsMessage)
        } else {
            // Queue message for later
            messageQueue.offer(wsMessage)
            Log.d(TAG, "Message queued (connection not ready): ${wsMessage.javaClass.simpleName}")
        }
    }

    private fun flushMessageQueue() {
        val scope = internalServerScope ?: return
        
        scope.launch {
            var message = messageQueue.poll()
            while (message != null) {
                if (connectionState.get() == ConnectionState.CONNECTED) {
                    sendMessageInternal(message)
                    Log.d(TAG, "Sent queued message: ${message.javaClass.simpleName}")
                } else {
                    // Put it back if we're not connected anymore
                    messageQueue.offer(message)
                    break
                }
                message = messageQueue.poll()
            }
        }
    }

    private fun handleDisconnection() {
        if (connectionState.get() == ConnectionState.DISCONNECTED) {
            return // Already handling disconnection
        }

        Log.d(TAG, "Handling disconnection")
        connectionState.set(ConnectionState.DISCONNECTED)
        
        // Cancel jobs
        readJob?.cancel()
        keepAliveJob?.cancel()
        
        // Close socket
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket: ${e.message}", e)
        }

        // Schedule reconnection
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        val scope = internalServerScope ?: return

        if (DEFAULT_MAX_RECONNECT_ATTEMPTS != -1 && reconnectAttempts >= DEFAULT_MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached ($DEFAULT_MAX_RECONNECT_ATTEMPTS)")
            return
        }

        connectionState.set(ConnectionState.RECONNECTING)
        reconnectAttempts++

        // Exponential backoff: 2s, 4s, 8s, 16s, max 60s
        val backoffDelay = min(
            DEFAULT_RECONNECT_DELAY_MS * (2.0.pow((reconnectAttempts - 1).toDouble())).toLong(),
            60000L
        )

        Log.d(TAG, "Scheduling reconnect attempt #$reconnectAttempts in ${backoffDelay}ms")

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            try {
                delay(backoffDelay)
                ensureActive()
                connect()
            } catch (e: CancellationException) {
                Log.d(TAG, "Reconnect cancelled")
            }
        }
    }

    internal fun destroy() {
        Log.d(TAG, "Destroy $TAG")
        
        connectionState.set(ConnectionState.DISCONNECTED)
        
        // Cancel all jobs
        readJob?.cancel()
        writeJob?.cancel()
        keepAliveJob?.cancel()
        reconnectJob?.cancel()
        
        // Close socket
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket: ${e.message}", e)
        }
        
        // Cancel scope
        internalServerScope?.cancel()
        internalServerScope = null
        
        // Close selector manager
        internalServerSelectorManager?.close()
        internalServerSelectorManager = null
        
        // Clear message queue
        messageQueue.clear()
        
        Log.d(TAG, "$TAG destroyed")
    }
}
