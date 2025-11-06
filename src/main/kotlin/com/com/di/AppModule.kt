package com.com.di

import com.com.ai.AiClient
import com.com.ai.ClaudeClient
import com.com.bot.FindTrackInteractor

object AppModule {

    private val aiClient: AiClient by lazy {
        ClaudeClient()
    }

    private val findTrackInteractor: FindTrackInteractor by lazy {
        FindTrackInteractor(aiClient)
    }

    fun provideAiClient(): AiClient = aiClient

    fun provideFindTrackInteractor(): FindTrackInteractor = findTrackInteractor
}
