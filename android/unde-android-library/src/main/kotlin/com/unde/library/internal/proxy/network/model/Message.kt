package com.unde.library.internal.proxy.network.model

import com.unde.library.internal.plugin.network.model.UndeRequestResponse
import com.unde.library.internal.constants.JsonTokenConstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Sealed interface representing all possible messages exchanged between the Android client and the Desktop server.
 *
 * This uses polymorphic serialization based on the "type" property.
 */
@Serializable
internal sealed interface Message {

    /**
     * Represents a plain receiving from the desktop tool.
     */
    @SerialName(JsonTokenConstant.TYPE_PLAIN_TOKEN)
    @Serializable
    data class Plain(
        /**
         * The plain payload as a string.
         */
        val data: String
    ) : Message

    /**
     * Represents captured network traffic data.
     */
    @SerialName(JsonTokenConstant.TYPE_NETWORK_TOKEN)
    @Serializable
    data class Network(
        /**
         * The detailed request and response information.
         */
        val data: UndeRequestResponse
    ) : Message

    /**
     * Represents database inspection data.
     */
    @SerialName(JsonTokenConstant.TYPE_DATABASE_TOKEN)
    @Serializable
    data class Database(
        /**
         * The database information payload.
         */
        val data: JsonObject
    ) : Message

    /**
     * Represents device telemetry data.
     */
    @SerialName(JsonTokenConstant.TYPE_TELEMETRY_TOKEN)
    @Serializable
    data class Telemetry(
        /**
         * The telemetry payload.
         */
        val data: JsonObject
    ) : Message

    /**
     * Represents device logcat output.
     */
    @SerialName(JsonTokenConstant.TYPE_LOGCAT_TOKEN)
    @Serializable
    data class Logcat(
        /**
         * The log entry payload.
         */
        val data: JsonObject
    ) : Message
}
