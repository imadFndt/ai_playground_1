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

             Follow this exact structure:
             {
              "title": "string - a brief title for your response",
              "randomBadThoughtAboutWriter": "string - a random bad thought about the writer",
              "answer": "string - your detailed response content"
            }

            Before outputting, validate that:
              - All brackets and braces are properly matched
              - All strings are properly quoted and escaped
              - No trailing commas exist
              - The JSON can be parsed by standard JSON parsers
              If you cannot provide a valid response, return:
              {\"error\": \"description of why response cannot be generated\", \"status\": \"failed\"}
          """.trimIndent()

        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(1000)
            .system(systemPrompt)
            .addUserMessage("Example")
            .addAssistantMessage("""{"title":"Example Title","randomBadThoughtAboutWriter":"They probably procrastinate","answer":"This is an example answer"}""")
            .addUserMessage("проверяю")
            .addAssistantMessage(
                """{
  "title": "Проверка связи",
  "randomBadThoughtAboutWriter": "Похоже, кто-то не может придумать ничего более оригинального, чем просто 'проверяю' — креативность на нуле",
  "answer": "Привет! Связь установлена, я тебя прекрасно слышу. Система работает нормально, все твои сообщения доходят. Можешь задавать любые вопросы или начинать обсуждение — я готов помочь с чем угодно: от сложных технических тем до философских размышлений. Чем займёмся?"
}"""
            )
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

    override suspend fun sendMessagePlainText(aiMessage: AiMessage): String {
        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(1000)
            .temperature(aiMessage.temperature)
            .addUserMessage(aiMessage.content)
            .build()

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
