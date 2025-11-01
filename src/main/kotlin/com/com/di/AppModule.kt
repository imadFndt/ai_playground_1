package com.com.di

import com.com.ai.AiClient
import com.com.ai.ClaudeClient

object AppModule {

    private val aiClient: AiClient by lazy {
        ClaudeClient()
    }

    fun provideAiClient(): AiClient = aiClient
}
