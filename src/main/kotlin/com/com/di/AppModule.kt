package com.com.di

import com.com.ai.AiClient
import com.com.ai.ClaudeClient
import com.com.ai.YandexGptClient
import com.com.bot.ExpertsInteractor
import com.com.bot.FindTrackInteractor
import com.com.bot.TemperatureInteractor

object AppModule {

    private val claudeClient: AiClient by lazy {
        ClaudeClient()
    }

    private val yandexGptClient: AiClient? by lazy {
        val apiKey = System.getenv("YANDEX_API_KEY")
        val folderId = System.getenv("YANDEX_FOLDER_ID")

        if (apiKey.isNullOrEmpty() || folderId.isNullOrEmpty()) {
            null
        } else {
            YandexGptClient()
        }
    }

    private val aiClient: AiClient by lazy {
        claudeClient
    }

    private val findTrackInteractor: FindTrackInteractor by lazy {
        FindTrackInteractor(aiClient)
    }

    private val expertsInteractor: ExpertsInteractor by lazy {
        ExpertsInteractor(claudeClient, yandexGptClient)
    }

    private val temperatureInteractor: TemperatureInteractor by lazy {
        TemperatureInteractor(claudeClient)
    }

    fun provideAiClient(): AiClient = aiClient

    fun provideClaudeClient(): AiClient = claudeClient

    fun provideYandexGptClient(): AiClient? = yandexGptClient

    fun provideFindTrackInteractor(): FindTrackInteractor = findTrackInteractor

    fun provideExpertsInteractor(): ExpertsInteractor = expertsInteractor

    fun provideTemperatureInteractor(): TemperatureInteractor = temperatureInteractor
}
