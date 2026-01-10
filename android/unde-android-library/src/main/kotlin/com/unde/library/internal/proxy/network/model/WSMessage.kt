package com.unde.library.internal.proxy.network.model

import com.unde.library.internal.plugin.network.model.UndeRequestResponse
import com.unde.library.internal.constants.JsonTokenConstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal sealed class WSMessage {
    @SerialName(JsonTokenConstant.TYPE_COMMAND_TOKEN)
    @Serializable
    data class Command(val data: JsonObject) : WSMessage()

    @SerialName(JsonTokenConstant.TYPE_NETWORK_TOKEN)
    @Serializable
    data class Network(val data: UndeRequestResponse) : WSMessage()

    @SerialName(JsonTokenConstant.TYPE_DATABASE_TOKEN)
    @Serializable
    data class Database(val data: JsonObject) : WSMessage()

    @SerialName(JsonTokenConstant.TYPE_TELEMETRY_TOKEN)
    @Serializable
    data class Telemetry(val data: JsonObject) : WSMessage()

    @SerialName(JsonTokenConstant.TYPE_LOGCAT_TOKEN)
    @Serializable
    data class Logcat(val data: JsonObject) : WSMessage()

    @SerialName(JsonTokenConstant.TYPE_KEEP_ALIVE_TOKEN)
    @Serializable
    data class KeepAlive(val timestamp: Long = System.currentTimeMillis()) : WSMessage()
}