package com.com.ai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class YandexGptClient(
    private val apiKey: String = System.getenv("YANDEX_API_KEY") ?: throw IllegalStateException("YANDEX_API_KEY environment variable is not set"),
    private val folderId: String = System.getenv("YANDEX_FOLDER_ID") ?: throw IllegalStateException("YANDEX_FOLDER_ID environment variable is not set"),
    private val model: String = "yandexgpt/latest"
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
        }
    }

    private val endpoint = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"

    override suspend fun sendMessageWithMetrics(aiMessage: AiMessage): AiResponseWithMetrics {
        val startTime = System.currentTimeMillis()

        val request = YandexGptRequest(
            modelUri = "gpt://$folderId/$model",
            completionOptions = CompletionOptions(
                stream = false,
                temperature = aiMessage.temperature,
                maxTokens = 10000
            ),
            messages = aiMessage.messages.map {
                Message(role = it.role, text = it.content)
            }
        )

        val response = client.post(endpoint) {
            header("Authorization", "Api-Key $apiKey")
            header("x-folder-id", folderId)
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val endTime = System.currentTimeMillis()

        val yandexResponse: YandexGptResponse = response.body()
        val content = yandexResponse.result.alternatives.firstOrNull()?.message?.text
            ?: throw IllegalStateException("No response from YandexGPT")

        return AiResponseWithMetrics(
            content = content,
            durationMs = endTime - startTime,
            promptTokens = yandexResponse.result.usage.inputTextTokens.toLongOrNull() ?: 0L,
            completionTokens = yandexResponse.result.usage.completionTokens.toLongOrNull() ?: 0L,
            totalTokens = yandexResponse.result.usage.totalTokens.toLongOrNull() ?: 0L
        )
    }

    fun close() {
        client.close()
    }
}

@Serializable
data class YandexGptRequest(
    val modelUri: String,
    val completionOptions: CompletionOptions,
    val messages: List<Message>
)

@Serializable
data class CompletionOptions(
    val stream: Boolean,
    val temperature: Double,
    val maxTokens: Int
)

@Serializable
data class Message(
    val role: String,
    val text: String
)

@Serializable
data class YandexGptResponse(
    val result: Result
)

@Serializable
data class Result(
    val alternatives: List<Alternative>,
    val usage: Usage,
    val modelVersion: String
)

@Serializable
data class Alternative(
    val message: Message,
    val status: String
)

@Serializable
data class Usage(
    @SerialName("inputTextTokens")
    val inputTextTokens: String,
    @SerialName("completionTokens")
    val completionTokens: String,
    @SerialName("totalTokens")
    val totalTokens: String
)
