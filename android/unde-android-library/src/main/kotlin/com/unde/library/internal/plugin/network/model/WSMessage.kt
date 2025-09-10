package com.unde.library.internal.plugin.network.model


import com.unde.server.constants.JsonTokenConstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface WSMessage {
    @Serializable
    @SerialName(JsonTokenConstant.TYPE_COMMAND_TOKEN)
    data class Command(val data: String) : WSMessage

    @Serializable
    @SerialName(JsonTokenConstant.TYPE_NETWORK_TOKEN)
    data class Network(val data: String) : WSMessage

    @Serializable
    @SerialName(JsonTokenConstant.TYPE_DATABASE_TOKEN)
    data class Database(val data: String) : WSMessage

    @Serializable
    @SerialName(JsonTokenConstant.TYPE_TELEMETRY_TOKEN)
    data class Telemetry(val data: String) : WSMessage

    @Serializable
    @SerialName(JsonTokenConstant.TYPE_LOGCAT_TOKEN)
    data class Logcat(val data: String) : WSMessage
}