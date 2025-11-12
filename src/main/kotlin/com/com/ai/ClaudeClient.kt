package com.com.ai

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.*
import com.anthropic.models.messages.MessageCreateParams

class ClaudeClient(
    private val model: String = "claude-sonnet-4-5-20250929",
) : AiClient {

    private val client: AnthropicClient by lazy {
        AnthropicOkHttpClient.fromEnv()
    }

    override suspend fun sendMessageWithMetrics(aiMessage: AiMessage): AiResponseWithMetrics {
        val startTime = System.currentTimeMillis()

        val paramsBuilder = MessageCreateParams.builder()
            .model(model)
            .maxTokens(1000)
            .temperature(aiMessage.temperature)

        // Add system message if present
        val systemMessages = aiMessage.messages.filter { it.role == "system" }
        if (systemMessages.isNotEmpty()) {
            paramsBuilder.system(systemMessages.joinToString("\n") { it.content })
        }

        // Add other messages in order
        aiMessage.messages
            .filter { it.role != "system" }
            .forEach { message ->
                when (message.role) {
                    "user" -> paramsBuilder.addUserMessage(message.content)
                    "assistant" -> paramsBuilder.addAssistantMessage(message.content)
                }
            }

        val params = paramsBuilder.build()
        val response = client.messages().create(params)

        val textContent = response.content()
            .mapNotNull { it.text().orElse(null) }
            .joinToString("") { it.text() }

        if (textContent.isEmpty()) {
            throw IllegalStateException("No text content in response")
        }

        val endTime = System.currentTimeMillis()

        return AiResponseWithMetrics(
            content = textContent,
            durationMs = endTime - startTime,
            promptTokens = response.usage().inputTokens(),
            completionTokens = response.usage().outputTokens(),
            totalTokens = response.usage().inputTokens() + response.usage().outputTokens()
        )
    }
}
