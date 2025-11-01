package com.com.ai

interface AiClient {
    suspend fun sendMessage(aiMessage: AiMessage): String
}

data class AiMessage(
    val role: String,
    val content: String
)
