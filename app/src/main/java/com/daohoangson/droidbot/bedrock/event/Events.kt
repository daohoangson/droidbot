package com.daohoangson.droidbot.bedrock.event

import com.daohoangson.droidbot.bedrock.MessageResponse
import com.daohoangson.droidbot.bedrock.message.StopReason
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject

// reference https://github.com/xemantic/anthropic-sdk-kotlin/blob/935ee04/src/commonMain/kotlin/event/Events.kt

@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
sealed class Event

@Serializable
@SerialName("message_start")
data class MessageStartEvent(
    val message: MessageResponse
) : Event()

@Serializable
@SerialName("message_delta")
data class MessageDeltaEvent(
    val delta: Delta,
    val usage: Usage
) : Event() {

    @Serializable
    data class Delta(
        @SerialName("stop_reason")
        val stopReason: StopReason,
        @SerialName("stop_sequence")
        val stopSequence: String? // TODO is that correct?
    )

}

@Serializable
@SerialName("message_stop")
class MessageStopEvent(
    @SerialName("amazon-bedrock-invocationMetrics")
    val amazonBedrockInvocationMetrics: InvocationMetrics
) : Event() {

    @Serializable
    data class InvocationMetrics(
        val firstByteLatency: Int,
        val inputTokenCount: Int,
        val invocationLatency: Int,
        val outputTokenCount: Int,
    )

}

// TODO error event is missing, should we rename all of these to events?

@Serializable
@SerialName("content_block_start")
data class ContentBlockStartEvent(
    val index: Int,
    @SerialName("content_block")
    val contentBlock: ContentBlock
) : Event()

@Serializable
@SerialName("content_block_stop")
data class ContentBlockStopEvent(
    val index: Int
) : Event()

@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
sealed class ContentBlock {

    @Serializable
    @SerialName("text")
    class Text(
        val text: String
    ) : ContentBlock()

    @Serializable
    @SerialName("tool_use")
    class ToolUse(
        val id: String,
        val input: JsonObject,
        val name: String
    ) : ContentBlock()
    // TODO missing tool_use
}

@Serializable
@SerialName("ping")
class PingEvent : Event() {
    override fun toString(): String = "Ping"
}

@Serializable
@SerialName("content_block_delta")
data class ContentBlockDeltaEvent(
    val index: Int,
    val delta: Delta
) : Event()

@Serializable
@JsonClassDiscriminator("type")
@OptIn(ExperimentalSerializationApi::class)
sealed class Delta {

    @Serializable
    @SerialName("text_delta")
    data class TextDelta(
        val text: String
    ) : Delta()

    @Serializable
    @SerialName("input_json_delta")
    data class InputJsonDelta(
        @SerialName("partial_json")
        val partialJson: String
    ) : Delta()

}

@Serializable
data class Usage(
    @SerialName("input_tokens")
    val inputTokens: Int? = null,
    @SerialName("output_tokens")
    val outputTokens: Int
)