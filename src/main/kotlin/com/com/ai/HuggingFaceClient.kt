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

    override suspend fun sendMessageJsonResponse(aiMessage: AiMessage): String {
        val systemPrompt = """
            You are a specialized assistant that MUST respond in valid JSON format only.
            IMPORTANT RULES:
            1. Your entire response must be valid, parseable JSON
            2. Start your response with { and end with }
            3. Do not include markdown code fences (no ```json)
            4. Do not include any text before or after the JSON object
            5. Properly escape all special characters in strings (quotes, backslashes, newlines)
            6. Use double quotes for all keys and string values
        """.trimIndent()

        val request = HuggingFaceRequest(
            model = modelName,
            messages = listOf(
                HuggingFaceMessage(role = "system", content = systemPrompt),
                HuggingFaceMessage(role = aiMessage.role, content = aiMessage.content)
            )
        )

        val response = client.post(endpoint) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val huggingFaceResponse: HuggingFaceResponse = response.body()
        return huggingFaceResponse.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("No response from HuggingFace")
    }

    override suspend fun sendMessagePlainText(aiMessage: AiMessage): String {
        val request = HuggingFaceRequest(
            model = modelName,
            messages = listOf(
                HuggingFaceMessage(role = aiMessage.role, content = aiMessage.content)
            )
        )

        val response = client.post(endpoint) {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val huggingFaceResponse: HuggingFaceResponse = response.body()
        return huggingFaceResponse.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("No response from HuggingFace")
    }

    override suspend fun sendMessageWithMetrics(aiMessage: AiMessage): AiResponseWithMetrics {
        val startTime = System.currentTimeMillis()

        val request = HuggingFaceRequest(
            model = modelName,
            messages = listOf(
                HuggingFaceMessage(role = aiMessage.role, content = aiMessage.content)
            )
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
            promptTokens = huggingFaceResponse.usage?.promptTokens ?: 0,
            completionTokens = huggingFaceResponse.usage?.completionTokens ?: 0,
            totalTokens = huggingFaceResponse.usage?.totalTokens ?: 0
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
