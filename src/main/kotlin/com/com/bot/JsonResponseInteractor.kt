package com.com.bot

import com.com.ai.AiClient
import com.com.ai.AiMessage
import com.com.ai.ConversationMessage

class JsonResponseInteractor(
    private val aiClient: AiClient
) {
    suspend fun getJsonResponse(aiMessage: AiMessage): String {
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
            {"error": "description of why response cannot be generated", "status": "failed"}
        """.trimIndent()

        // Prepend system message to existing messages
        val messagesWithSystem = listOf(
            ConversationMessage("system", systemPrompt)
        ) + aiMessage.messages

        val messageWithSystemPrompt = AiMessage(
            messages = messagesWithSystem,
            temperature = aiMessage.temperature
        )

        val response = aiClient.sendMessageWithMetrics(messageWithSystemPrompt)
        return response.content
    }
}
