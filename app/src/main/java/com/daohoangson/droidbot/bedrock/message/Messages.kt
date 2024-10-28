@file:OptIn(ExperimentalSerializationApi::class)

package com.daohoangson.droidbot.bedrock.message

import kotlinx.serialization.*
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject

// references https://github.com/xemantic/anthropic-sdk-kotlin/blob/935ee04/src/commonMain/kotlin/message/Messages.kt

@Serializable
enum class Role {
    @SerialName("user")
    USER,

    @SerialName("assistant")
    ASSISTANT
}

@Serializable
data class Metadata(
    @SerialName("user_id")
    val userId: String
)

@Serializable
data class MessageRequest(
    @SerialName("anthropic_beta")
    val anthropicBeta: List<String> = listOf("computer-use-2024-10-22"),

    @SerialName("anthropic_version")
    val anthropicVersion: String = "bedrock-2023-05-31",

    @SerialName("max_tokens")
    val maxTokens: Int = 8192,

    val messages: List<Message>,

    val system: String = "The user will ask you to perform a task and you should use their computer to do so. After each step, take a screenshot and carefully evaluate if you have achieved the right outcome. Explicitly show your thinking: \"I have evaluated step X...\" If not correct, try again. Only when you confirm a step was executed correctly should you move on to the next one. Note that you have to click into the browser address bar before typing a URL. You should always call a tool! Always return a tool call. Remember call the finish_run tool when you have achieved the goal of the task. Do not explain you have finished the task, just call the tool. Use keyboard shortcuts to navigate whenever possible.",

    val tools: List<Tool>?,
)

@Serializable
data class Message(
    val role: Role,
    val content: List<Content>
)

@Serializable
@JsonClassDiscriminator("name")
sealed class Tool()

@Serializable
@SerialName("computer")
data class ComputerTool(
    @SerialName("display_height_px")
    val displayHeightPx: Int,

    @SerialName("display_number")
    val displayNumber: Int = 1,

    @SerialName("display_width_px")
    val displayWidthPx: Int,

    val type: String = "computer_20241022",
) : Tool()

@Serializable
@JsonClassDiscriminator("type")
sealed class Content

@Serializable
@SerialName("text")
data class Text(
    val text: String,
) : Content()

@Serializable
@SerialName("image")
data class Image(
    val source: Source,
) : Content() {

    enum class MediaType {
        @SerialName("image/jpeg")
        IMAGE_JPEG,

        @SerialName("image/png")
        IMAGE_PNG,

        @SerialName("image/gif")
        IMAGE_GIF,

        @SerialName("image/webp")
        IMAGE_WEBP
    }

    @Serializable
    data class Source(
        val type: Type = Type.BASE64,
        @SerialName("media_type")
        val mediaType: MediaType,
        val data: String
    ) {

        enum class Type {
            @SerialName("base64")
            BASE64
        }

    }

}

@SerialName("tool_use")
@Serializable
data class ToolUse(
    val id: String,
    val name: String,
    val input: JsonObject
) : Content()

@Serializable
@SerialName("tool_result")
data class ToolResult(
    @SerialName("tool_use_id")
    val toolUseId: String,
    val content: List<Content>, // TODO only Text, Image allowed here, should be accessible in gthe builder
    @SerialName("is_error")
    val isError: Boolean = false,
) : Content()

fun ToolResult(
    toolUseId: String,
    text: String
): ToolResult = ToolResult(
    toolUseId,
    content = listOf(Text(text))
)

@Serializable
data class CacheControl(
    val type: Type
) {

    enum class Type {
        @SerialName("ephemeral")
        EPHEMERAL
    }

}

@Serializable
@JsonClassDiscriminator("type")
sealed class ToolChoice(
    @SerialName("disable_parallel_tool_use")
    val disableParallelToolUse: Boolean = false
) {

    @Serializable
    @SerialName("auto")
    class Auto : ToolChoice()

    @Serializable
    @SerialName("any")
    class Any : ToolChoice()

    @Serializable
    @SerialName("tool")
    class Tool(
        val name: String
    ) : ToolChoice()

}

@Serializable
enum class StopReason {
    @SerialName("end_turn")
    END_TURN,

    @SerialName("max_tokens")
    MAX_TOKENS,

    @SerialName("stop_sequence")
    STOP_SEQUENCE,

    @SerialName("tool_use")
    TOOL_USE
}

@Serializable
data class Usage(
    @SerialName("input_tokens")
    val inputTokens: Int,
    @SerialName("cache_creation_input_tokens")
    val cacheCreationInputTokens: Int? = null,
    @SerialName("cache_read_input_tokens")
    val cacheReadInputTokens: Int? = null,
    @SerialName("output_tokens")
    val outputTokens: Int
)
