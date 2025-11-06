package com.com.ai

interface AiClient {
    suspend fun sendMessageJsonResponse(aiMessage: AiMessage): String
    suspend fun sendMessagePlainText(aiMessage: AiMessage): String
}

data class AiMessage(
    val role: String,
    val content: String,
    val temperature: Double = 1.0
) {
    init {
        require(temperature in 0.0..1.0) { "Temperature must be between 0.0 and 1.0, got $temperature" }
    }
}
