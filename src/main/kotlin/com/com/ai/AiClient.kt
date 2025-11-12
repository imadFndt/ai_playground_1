package com.com.ai

interface AiClient {
    suspend fun sendMessageWithMetrics(aiMessage: AiMessage): AiResponseWithMetrics
}

// Extension function for plain text responses
suspend fun AiClient.sendMessagePlainText(aiMessage: AiMessage): String {
    return sendMessageWithMetrics(aiMessage).content
}

data class ConversationMessage(
    val role: String,
    val content: String
)

data class AiMessage(
    val messages: List<ConversationMessage>,
    val temperature: Double = 1.0
) {
    // Convenience constructor for single message
    constructor(role: String, content: String, temperature: Double = 1.0) : this(
        messages = listOf(ConversationMessage(role, content)),
        temperature = temperature
    )

    init {
        require(temperature in 0.0..1.0) { "Temperature must be between 0.0 and 1.0, got $temperature" }
        require(messages.isNotEmpty()) { "Messages list cannot be empty" }
    }
}

data class AiResponseWithMetrics(
    val content: String,
    val durationMs: Long,
    val promptTokens: Long,
    val completionTokens: Long,
    val totalTokens: Long
)
