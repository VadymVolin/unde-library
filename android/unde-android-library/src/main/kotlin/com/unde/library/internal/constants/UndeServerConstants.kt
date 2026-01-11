package com.unde.library.internal.constants

import kotlin.time.Duration.Companion.seconds

internal const val DEFAULT_REAL_SERVER_HOST = "127.0.0.1"
internal const val DEFAULT_EMULATOR_SERVER_HOST = "10.0.2.2"
internal val DEFAULT_CONNECTION_TIMEOUT_SEC = 15L.seconds
internal val DEFAULT_PING_INTERVAL_SEC = 3L.seconds

// Socket configuration
internal const val DEFAULT_SERVER_SOCKET_PORT = 8080
internal const val DEFAULT_KEEP_ALIVE_INTERVAL_MS = 5000L
internal const val DEFAULT_RECONNECT_DELAY_MS = 3000L
internal const val DEFAULT_KEEP_ALIVE_TIMEOUT_MS = 15000L