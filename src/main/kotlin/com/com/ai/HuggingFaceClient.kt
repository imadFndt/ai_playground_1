package com.com.ai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class HuggingFaceClient(
    private val apiKey: String = System.getenv("HUGGING_FACE_API_KEY") ?: throw IllegalStateException("HUGGING_FACE_API_KEY environment variable is not set"),
    private val modelName: String = "mistralai/Mistral-7B-Instruct-v0.2"
) : AiClient {

    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                connectTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            }
        }
    }

    private val endpoint = "https://router.huggingface.co/v1/chat/completions"

    override suspend fun sendMessageWithMetrics(aiMessage: AiMessage): AiResponseWithMetrics {
        val startTime = System.currentTimeMillis()

        val request = HuggingFaceRequest(
            model = modelName,
            messages = aiMessage.messages.map {
                HuggingFaceMessage(role = it.role, content = it.content)
            }
        )

        val response = client.post(endpoint) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val endTime = System.currentTimeMillis()
        val durationMs = endTime - startTime

        val huggingFaceResponse: HuggingFaceResponse = response.body()
        val content = huggingFaceResponse.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("No response from HuggingFace")

        return AiResponseWithMetrics(
            content = content,
            durationMs = durationMs,
            promptTokens = huggingFaceResponse.usage?.promptTokens?.toLong() ?: 0L,
            completionTokens = huggingFaceResponse.usage?.completionTokens?.toLong() ?: 0L,
            totalTokens = huggingFaceResponse.usage?.totalTokens?.toLong() ?: 0L
        )
    }

    fun close() {
        client.close()
    }
}

@Serializable
data class HuggingFaceRequest(
    val model: String,
    val messages: List<HuggingFaceMessage>
)

@Serializable
data class HuggingFaceMessage(
    val role: String,
    val content: String
)

@Serializable
data class HuggingFaceResponse(
    val choices: List<HuggingFaceChoice>,
    val usage: HuggingFaceUsage? = null
)

@Serializable
data class HuggingFaceChoice(
    val message: HuggingFaceMessage
)

@Serializable
data class HuggingFaceUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int? = null,
    @SerialName("completion_tokens")
    val completionTokens: Int? = null,
    @SerialName("total_tokens")
    val totalTokens: Int? = null
)
