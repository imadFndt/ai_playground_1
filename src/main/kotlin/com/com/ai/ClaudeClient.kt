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

    override suspend fun sendMessage(aiMessage: AiMessage): String {
        val params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(1000)
                .addUserMessage(aiMessage.content)
                .build();

        val response = client.messages().create(params)

        val textContent = response.content()
            .mapNotNull { it.text().orElse(null) }
            .joinToString("") { it.text() }

        if (textContent.isEmpty()) {
            throw IllegalStateException("No text content in response")
        }

        return textContent
    }
}
