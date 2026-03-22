package com.unde.library.internal.proxy.network.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SessionManagerTest {

    @Before
    fun setup() {
        SessionManager.reset()
    }

    @Test
    fun testClientIdIsUniqueAndPersistent() {
        val initialId = SessionManager.clientId
        assertNotNull(initialId)
        
        // Re-accessing it doesn't change it unless reset
        assertEquals(initialId, SessionManager.clientId)
    }

    @Test
    fun testSessionIdLifecycle() {
        assertNull(SessionManager.sessionId)

        // Simulate server assigning a session id
        SessionManager.sessionId = "server-session-123"
        assertEquals("server-session-123", SessionManager.sessionId)

        // Simulate server rejecting session (clearSession)
        SessionManager.clearSession()
        assertNull("Session ID should be cleared", SessionManager.sessionId)
        assertNotNull("Client ID must persist when session is cleared", SessionManager.clientId)
    }

    @Test
    fun testReset() {
        val oldClientId = SessionManager.clientId
        SessionManager.sessionId = "temp-session"

        SessionManager.reset()

        val newClientId = SessionManager.clientId
        assertNotEquals("Reset should generate a new clientId", oldClientId, newClientId)
        assertNull("Reset should also clear the sessionId", SessionManager.sessionId)
    }
}
