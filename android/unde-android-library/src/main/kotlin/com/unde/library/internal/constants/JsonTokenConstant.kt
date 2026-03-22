package com.unde.library.internal.constants

/**
 * Constants used for JSON serialization and message type discrimination.
 */
internal object JsonTokenConstant {
    // type json token
    /**
     * Key used in JSON to identify the message type.
     */
    const val TYPE_TOKEN = "type"
    // values

    /**
     * Value indicating a plain message.
     */
    const val TYPE_PLAIN_TOKEN = "plain"

    /**
     * Value indicating a network traffic message.
     */
    const val TYPE_NETWORK_TOKEN = "network"

    /**
     * Value indicating a database inspection message.
     */
    const val TYPE_DATABASE_TOKEN = "database"

    /**
     * Value indicating a telemetry message.
     */
    const val TYPE_TELEMETRY_TOKEN = "telemetry"

    /**
     * Value indicating a logcat message.
     */
    const val TYPE_LOGCAT_TOKEN = "logcat"

    /**
     * Value indicating a session init message.
     */
    const val TYPE_SESSION_INIT_TOKEN = "session_init"

    /**
     * Value indicating a session resume message.
     */
    const val TYPE_SESSION_RESUME_TOKEN = "session_resume"

    /**
     * Value indicating a session ack message.
     */
    const val TYPE_SESSION_ACK_TOKEN = "session_ack"
}
