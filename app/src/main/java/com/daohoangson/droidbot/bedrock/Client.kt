package com.daohoangson.droidbot.bedrock

import android.util.Log
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.bedrockruntime.BedrockRuntimeClient
import aws.sdk.kotlin.services.bedrockruntime.model.InvokeModelWithResponseStreamRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials.Companion.invoke
import com.daohoangson.droidbot.bedrock.computer.ComputerUseEvent
import com.daohoangson.droidbot.bedrock.computer.ComputerUseInput
import com.daohoangson.droidbot.bedrock.event.ContentBlock
import com.daohoangson.droidbot.bedrock.event.ContentBlockDeltaEvent
import com.daohoangson.droidbot.bedrock.event.ContentBlockStartEvent
import com.daohoangson.droidbot.bedrock.event.ContentBlockStopEvent
import com.daohoangson.droidbot.bedrock.event.Delta
import com.daohoangson.droidbot.bedrock.event.Event
import com.daohoangson.droidbot.bedrock.event.MessageStartEvent
import com.daohoangson.droidbot.bedrock.event.MessageStopEvent
import com.daohoangson.droidbot.bedrock.message.ComputerTool
import com.daohoangson.droidbot.bedrock.message.Message
import com.daohoangson.droidbot.bedrock.message.MessageRequest
import com.daohoangson.droidbot.bedrock.message.Role
import com.daohoangson.droidbot.bedrock.message.Text
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Client(val accessKeyId: String, val secretAccessKey: String) {
    private val awsClient: BedrockRuntimeClient by lazy {
        return@lazy BedrockRuntimeClient {
            val credentials = Credentials(
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey,
            )
            credentialsProvider = StaticCredentialsProvider(credentials)
            region = "us-west-2"
        }
    }

    private val json = Json {
        encodeDefaults = true
    }

    fun invokeModel(requestBody: MessageRequest): Flow<Event> = channelFlow {
        val encodedBody = json.encodeToString(requestBody)
        Log.v("invokeModel", "request: $encodedBody")
        val request = InvokeModelWithResponseStreamRequest.invoke {
            modelId = "anthropic.claude-3-5-sonnet-20241022-v2:0"
            body = encodedBody.toByteArray()
        }

        val eventSerializer = Event.serializer()
        awsClient.invokeModelWithResponseStream(request) { response ->
            response.body?.collect { responseStream ->
                responseStream.asChunkOrNull()?.let { chunk ->
                    chunk.bytes?.decodeToString()?.let { str ->
                        try {
                            send(json.decodeFromString(eventSerializer, str))
                        } catch (e: Exception) {
                            Log.e("invokeModel", "chunk: $str", e)
                        }
                    }
                }
            }
        }
    }

    fun invokeComputerUse(
        displayHeightPx: Int,
        displayWidthPx: Int,
        messages: List<Message>,
    ): Flow<ComputerUseEvent> = channelFlow {
        val computerTool = ComputerTool(
            displayHeightPx = displayHeightPx,
            displayWidthPx = displayWidthPx,
        )
        val requestBody = MessageRequest(
            messages = messages,
            tools = listOf(computerTool),
        )

        var contentBlock: ContentBlock? = null
        var index = -1
        var message: Message? = null
        val computerUseInputSerializer = ComputerUseInput.serializer()
        invokeModel(requestBody = requestBody).collect { event ->
            when (event) {
                is MessageStartEvent -> {
                    message = Message(
                        role = event.message.role,
                        content = event.message.content
                    )
                }

                is ContentBlockStartEvent -> {
                    contentBlock = event.contentBlock
                    index = event.index
                }

                is ContentBlockDeltaEvent -> {
                    if (event.index != index) {
                        throw IllegalStateException("Unexpected $event: index=$index")
                    }

                    event.delta.let { delta ->
                        when (delta) {
                            is Delta.TextDelta -> {
                                val existing = contentBlock
                                if (existing is ContentBlock.Text) {
                                    contentBlock = existing.copy(text = existing.text + delta.text)
                                    Log.d("invokeComputerUse", "delta.text: ${delta.text}")
                                } else {
                                    throw IllegalStateException("Unexpected $event: existing=$existing")
                                }
                            }

                            is Delta.InputJsonDelta -> {
                                val existing = contentBlock
                                if (existing is ContentBlock.ToolUse) {
                                    contentBlock =
                                        existing.copy(inputJson = existing.inputJson + delta.partialJson)
                                } else {
                                    throw IllegalStateException("Unexpected $event: existing=$existing")
                                }
                            }
                        }
                    }
                }

                is ContentBlockStopEvent -> {
                    val existing = contentBlock
                    if (event.index != index || existing == null) {
                        throw IllegalStateException("Unexpected $event: index=$index existing=$existing")
                    }

                    when (existing) {
                        is ContentBlock.Text -> {
                            send(ComputerUseEvent.Text(text = existing.text))
                        }

                        is ContentBlock.ToolUse -> {
                            when (existing.name) {
                                "computer" -> {
                                    try {
                                        val input = json.decodeFromString(
                                            computerUseInputSerializer, existing.inputJson
                                        )
                                        send(
                                            ComputerUseEvent.ComputerUse(
                                                id = existing.id,
                                                input = input,
                                                name = existing.name,
                                            )
                                        )
                                    } catch (e: Exception) {
                                        Log.e(
                                            "invokeComputerUse",
                                            "input json: ${existing.inputJson}",
                                            e
                                        )
                                    }
                                }

                                else -> {
                                    throw IllegalStateException("Unexpected tool: $existing")
                                }
                            }
                        }
                    }
                }

                is MessageStopEvent -> {
                    val existing = message
                    if (existing == null) {
                        throw IllegalStateException("Unexpected $event: existing=null")
                    }
                    send(ComputerUseEvent.MessageStopEvent(message = existing))
                }

                else -> {
                    Log.v("invokeComputerUse", "event: $event")
                }
            }
        }
    }
}