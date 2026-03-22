package com.unde.library.internal.proxy.network.session

import java.util.UUID

/**
 * Manages the session identity for the Android client.
 *
 * It holds a unique client identifier per process and an optional session identifier
 * assigned by the server. It also provides methods to clear or reset the session state.
 */
internal object SessionManager {
    /**
     * A unique identifier for this client process, generated once.
     */
    var clientId: String = UUID.randomUUID().toString()
        private set

    /**
     * The session identifier assigned by the server, if any.
     */
    var sessionId: String? = null

    /**
     * Clears the current session assignation. This forces a new session to be created
     * on the next connection attempt.
     */
    fun clearSession() {
        sessionId = null
    }

    /**
     * Resets the entire session state, generating a new client identifier and clearing
     * the session identifier.
     */
    fun reset() {
        clientId = UUID.randomUUID().toString()
        clearSession()
    }
}
