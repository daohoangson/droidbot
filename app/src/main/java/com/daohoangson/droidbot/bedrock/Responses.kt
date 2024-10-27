package com.daohoangson.droidbot.bedrock

import com.daohoangson.droidbot.bedrock.event.Usage
import com.daohoangson.droidbot.bedrock.message.Content
import com.daohoangson.droidbot.bedrock.message.Message
import com.daohoangson.droidbot.bedrock.message.Role
import com.daohoangson.droidbot.bedrock.message.StopReason
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

// reference https://github.com/xemantic/anthropic-sdk-kotlin/blob/935ee04/src/commonMain/kotlin/Responses.kt

@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
sealed class Response(
    val type: String
)

@Serializable
@SerialName("error")
data class ErrorResponse(
    val error: Error
) : Response(type = "error")

@Serializable
@SerialName("message")
data class MessageResponse(
    val id: String,
    val role: Role,
    val content: List<Content>, // limited to Text and ToolUse
    val model: String,
    @SerialName("stop_reason")
    val stopReason: StopReason?,
    @SerialName("stop_sequence")
    val stopSequence: String?,
    val usage: Usage
) : Response(type = "message") {

    fun asMessage(): Message =
        Message(
            role = Role.ASSISTANT,
            content = this@MessageResponse.content,
        )

}

@Serializable
data class Error(
    val type: String, val message: String
)