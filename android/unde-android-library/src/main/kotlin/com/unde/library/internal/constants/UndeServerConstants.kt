package com.unde.library.internal.constants

import kotlin.time.Duration.Companion.seconds

/**
 * Default localhost IP for real devices / standard emulators when port forwarding is used.
 */
internal const val DEFAULT_REAL_SERVER_HOST = "127.0.0.1"

/**
 * Default IP address for Android Emulator to access the host machine's localhost (10.0.2.2).
 */
internal const val DEFAULT_EMULATOR_SERVER_HOST = "10.0.2.2"

/**
 * Default timeout for socket connection attempts.
 */
internal val DEFAULT_CONNECTION_TIMEOUT_SEC = 15L.seconds

/**
 * Default interval for sending ping signals to keep the connection alive.
 */
internal val DEFAULT_PING_INTERVAL_SEC = 3L.seconds

// Socket configuration

/**
 * The port number used for the Unde server socket connection (default: 8081).
 */
internal const val DEFAULT_SERVER_SOCKET_PORT = 8081

/**
 * Delay in milliseconds before attempting to reconnect to the server after a disconnection.
 */
internal const val DEFAULT_RECONNECT_DELAY_MS = 3000L

/**
 * Default timeout to get ping-pong result from the server.
 */
internal const val DEFAULT_PING_TIMEOUT_MS = 5000L

/**
 * Default max frame size 50mb
 */
internal const val DEFAULT_MAX_FRAME_SIZE = 50L * 1024 * 1024


