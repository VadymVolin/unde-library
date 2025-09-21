package com.unde.library.internal.plugin.network.model


import com.unde.server.constants.JsonTokenConstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
internal sealed class WSMessage {
    @SerialName(JsonTokenConstant.TYPE_COMMAND_TOKEN)
    @Serializable
    data class Command(val data: String) : WSMessage()

    @SerialName(JsonTokenConstant.TYPE_NETWORK_TOKEN)
    @Serializable
    data class Network(val data: UndeRequestResponse) : WSMessage()

    @SerialName(JsonTokenConstant.TYPE_DATABASE_TOKEN)
    @Serializable
    data class Database(val data: String) : WSMessage()

    @SerialName(JsonTokenConstant.TYPE_TELEMETRY_TOKEN)
    @Serializable
    data class Telemetry(val data: String) : WSMessage()

    @SerialName(JsonTokenConstant.TYPE_LOGCAT_TOKEN)
    @Serializable
    data class Logcat(val data: String) : WSMessage()
}
//
//val wsMessageModule = SerializersModule {
//    polymorphic(WSMessage::class) {
//        subclass(WSMessage.Command::class, WSMessage.Command.serializer())
//        subclass(WSMessage.Network::class, WSMessage.Network.serializer())
//        subclass(WSMessage.Database::class, WSMessage.Database.serializer())
//        subclass(WSMessage.Telemetry::class, WSMessage.Telemetry.serializer())
//        subclass(WSMessage.Logcat::class, WSMessage.Logcat.serializer())
//    }
//}