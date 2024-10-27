package com.daohoangson.droidbot.bedrock

import android.util.Log
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.bedrockruntime.BedrockRuntimeClient
import aws.sdk.kotlin.services.bedrockruntime.model.InvokeModelWithResponseStreamRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials.Companion.invoke

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

    suspend fun invokeModelWithResponseStream() {
        val request = InvokeModelWithResponseStreamRequest.invoke {
            modelId = "anthropic.claude-3-5-sonnet-20241022-v2:0"
            body = """{
    "anthropic_version": "bedrock-2023-05-31", 
    "anthropic_beta": ["computer-use-2024-10-22"],
    "max_tokens": 8192,
    "messages": [
        {
            "role": "user",
            "content": [
                {
                    "type": "text",
                    "text": "What is the answer to life, the universe, and everything?"
                }
            ]
        }
    ]
}""".toByteArray()

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