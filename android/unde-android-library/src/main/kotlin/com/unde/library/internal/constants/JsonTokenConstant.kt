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
     * Value indicating a generic result message.
     */
    const val TYPE_RESULT_TOKEN = "result"

    /**
     * Value indicating a command message.
     */
    const val TYPE_COMMAND_TOKEN = "command"

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

    // next...
}
