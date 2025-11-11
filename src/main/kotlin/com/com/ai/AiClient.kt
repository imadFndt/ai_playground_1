package com.com.ai

interface AiClient {
    suspend fun sendMessageJsonResponse(aiMessage: AiMessage): String
    suspend fun sendMessagePlainText(aiMessage: AiMessage): String
    suspend fun sendMessageWithMetrics(aiMessage: AiMessage): AiResponseWithMetrics {
        val startTime = System.currentTimeMillis()
        val content = sendMessagePlainText(aiMessage)
        val endTime = System.currentTimeMillis()

        return AiResponseWithMetrics(
            content = content,
            durationMs = endTime - startTime,
            promptTokens = 0,
            completionTokens = 0,
            totalTokens = 0
        )
    }
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

data class AiResponseWithMetrics(
    val content: String,
    val durationMs: Long,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)
