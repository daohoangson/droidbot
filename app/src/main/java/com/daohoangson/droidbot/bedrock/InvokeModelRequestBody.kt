package com.daohoangson.droidbot.bedrock

import com.xemantic.anthropic.message.Message
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalSerializationApi()
@Serializable
data class InvokeModelRequestBody(
    @EncodeDefault
    @ExperimentalSerializationApi
    @SerialName("anthropic_beta")
    val anthropicBeta: List<String> = listOf("computer-use-2024-10-22"),

    @EncodeDefault
    @ExperimentalSerializationApi
    @SerialName("anthropic_version")
    val anthropicVersion: String = "bedrock-2023-05-31",

    @EncodeDefault
    @ExperimentalSerializationApi
    @SerialName("max_tokens")
    val maxTokens: Int = 8192,

    val messages: List<Message>,
)
