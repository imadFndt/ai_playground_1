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

    override suspend fun sendMessageJsonResponse(aiMessage: AiMessage): String {
        val systemPrompt = """
            You are a specialized assistant that MUST respond in valid JSON format only.
            IMPORTANT RULES:
            1. Your entire response must be valid, parseable JSON
            2. Start your response with U+005B and end with U+005D
            3. Do not include markdown code fences (no ```json)
            4. Do not include any text before or after the JSON object
            5. Properly escape all special characters in strings (quotes, backslashes, newlines)
            6. Use double quotes for all keys and string values
        """.trimIndent()

        val request = YandexGptRequest(
            modelUri = "gpt://$folderId/$model",
            completionOptions = CompletionOptions(
                stream = false,
                temperature = aiMessage.temperature,
                maxTokens = 10000
            ),
            messages = listOf(
                Message(role = "system", text = systemPrompt),
                Message(role = "user", text = aiMessage.content)
            )
        )

        val response = client.post(endpoint) {
            header("Authorization", "Api-Key $apiKey")
            header("x-folder-id", folderId)
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val yandexResponse: YandexGptResponse = response.body()
        return yandexResponse.result.alternatives.firstOrNull()?.message?.text
            ?: throw IllegalStateException("No response from YandexGPT")
    }

    override suspend fun sendMessagePlainText(aiMessage: AiMessage): String {
        val request = YandexGptRequest(
            modelUri = "gpt://$folderId/$model",
            completionOptions = CompletionOptions(
                stream = false,
                temperature = aiMessage.temperature,
                maxTokens = 100
            ),
            messages = listOf(
                Message(role = aiMessage.role, text = aiMessage.content)
            )
        )

        val response = client.post(endpoint) {
            header("Authorization", "Api-Key $apiKey")
            header("x-folder-id", folderId)
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val yandexResponse: YandexGptResponse = response.body()
        return yandexResponse.result.alternatives.firstOrNull()?.message?.text
            ?: throw IllegalStateException("No response from YandexGPT")
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
