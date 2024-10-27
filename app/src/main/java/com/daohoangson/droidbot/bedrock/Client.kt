package com.daohoangson.droidbot.bedrock

import android.util.Log
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.bedrockruntime.BedrockRuntimeClient
import aws.sdk.kotlin.services.bedrockruntime.model.InvokeModelWithResponseStreamRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials.Companion.invoke
import com.daohoangson.droidbot.bedrock.event.Event
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

    fun invokeModel(
        displayHeightPx: Int,
        displayWidthPx: Int
    ): Flow<Event> = channelFlow {
        val message = Message(
            role = Role.USER,
            content = listOf(Text("Search for near by pizza place and tell me the top 3 with best reviews."))
        )
        val computerTool = ComputerTool(
            displayHeightPx = displayHeightPx,
            displayWidthPx = displayWidthPx,
        )
        val requestBody = MessageRequest(
            messages = listOf(message),
            tools = listOf(computerTool),
        )
        val encodedBody = json.encodeToString(requestBody)
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
                            Log.e("Bedrock", "chunk: $str", e)
                        }
                    }
                }
            }
        }
    }
}