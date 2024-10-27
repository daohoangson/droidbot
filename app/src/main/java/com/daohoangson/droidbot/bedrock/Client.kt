package com.daohoangson.droidbot.bedrock

import android.util.Log
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.bedrockruntime.BedrockRuntimeClient
import aws.sdk.kotlin.services.bedrockruntime.model.InvokeModelWithResponseStreamRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials.Companion.invoke
import com.xemantic.anthropic.message.Message
import kotlinx.serialization.InternalSerializationApi
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

    @OptIn(InternalSerializationApi::class)
    suspend fun invokeModelWithResponseStream() {
        val message = Message { +"What is the answer to life, the universe, and everything?" }
        val requestBody = InvokeModelRequestBody(messages = listOf(message))
        val encodedBody = Json.encodeToString(requestBody)
        val request = InvokeModelWithResponseStreamRequest.invoke {
            modelId = "anthropic.claude-3-5-sonnet-20241022-v2:0"
            body = encodedBody.toByteArray()
        }

        awsClient.invokeModelWithResponseStream(request) {
            it.body?.collect {
                it.asChunkOrNull()?.apply {
                    bytes?.decodeToString()?.apply { Log.d("Bedrock", this) }
                }
            }
        }
    }
}