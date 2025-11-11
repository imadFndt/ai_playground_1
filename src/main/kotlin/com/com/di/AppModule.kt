package com.com.di

import com.com.ai.AiClient
import com.com.ai.ClaudeClient
import com.com.ai.YandexGptClient
import com.com.ai.HuggingFaceClient
import com.com.bot.ExpertsInteractor
import com.com.bot.FindTrackInteractor
import com.com.bot.TemperatureInteractor
import com.com.bot.DifferentModelsInteractor

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

    private fun createHuggingFaceClient(modelName: String): AiClient? {
        val apiKey = System.getenv("HUGGING_FACE_API_KEY")
        return if (apiKey.isNullOrEmpty()) {
            null
        } else {
            HuggingFaceClient(apiKey, modelName)
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

    private val differentModelsInteractor: DifferentModelsInteractor? by lazy {
        val modelA = createHuggingFaceClient("moonshotai/Kimi-K2-Thinking")
        val modelB = createHuggingFaceClient("meta-llama/Meta-Llama-3-8B-Instruct")
        val modelC = createHuggingFaceClient("Qwen/Qwen2.5-7B-Instruct")

        if (modelA != null && modelB != null && modelC != null) {
            DifferentModelsInteractor(modelA, modelB, modelC)
        } else {
            null
        }
    }

    fun provideAiClient(): AiClient = aiClient

    fun provideClaudeClient(): AiClient = claudeClient

    fun provideYandexGptClient(): AiClient? = yandexGptClient

    fun provideFindTrackInteractor(): FindTrackInteractor = findTrackInteractor

    fun provideExpertsInteractor(): ExpertsInteractor = expertsInteractor

    fun provideTemperatureInteractor(): TemperatureInteractor = temperatureInteractor

    fun provideDifferentModelsInteractor(): DifferentModelsInteractor? = differentModelsInteractor
}
