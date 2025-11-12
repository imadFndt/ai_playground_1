package com.com.bot

import com.com.ai.AiClient
import com.com.ai.AiMessage
import com.com.ai.AiResponseWithMetrics
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode

class DifferentModelsInteractor(
    private val modelAClient: AiClient,
    private val modelBClient: AiClient,
    private val modelCClient: AiClient
) {

    suspend fun processQuestion(bot: Bot, chatId: Long, question: String) {
        bot.sendChatAction(chatId = ChatId.fromId(chatId), action = ChatAction.TYPING)

        try {
            sendMessage(bot, chatId, "🤖 *Processing your question with 3 different HuggingFace models*\n\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━━")

            // Process with Model A
            sendMessage(bot, chatId, "\n🔵 *Model A: moonshotai/Kimi-K2-Thinking*")
            bot.sendChatAction(chatId = ChatId.fromId(chatId), action = ChatAction.TYPING)

            try {
                val modelAResponse = modelAClient.sendMessageWithMetrics(AiMessage("user", question, 0.7))
                sendMessage(bot, chatId, modelAResponse.content)
                sendMessage(bot, chatId, formatMetrics(modelAResponse.durationMs, modelAResponse.promptTokens, modelAResponse.completionTokens, modelAResponse.totalTokens))
            } catch (e: Exception) {
                sendMessage(bot, chatId, "❌ Error: ${e.message}")
            }

            // Process with Model B
            sendMessage(bot, chatId, "\n🟢 *Model B: Meta-Llama-3-8B-Instruct*")
            bot.sendChatAction(chatId = ChatId.fromId(chatId), action = ChatAction.TYPING)

            try {
                val modelBResponse = modelBClient.sendMessageWithMetrics(AiMessage("user", question, 0.7))
                sendMessage(bot, chatId, modelBResponse.content)
                sendMessage(bot, chatId, formatMetrics(modelBResponse.durationMs, modelBResponse.promptTokens, modelBResponse.completionTokens, modelBResponse.totalTokens))
            } catch (e: Exception) {
                sendMessage(bot, chatId, "❌ Error: ${e.message}")
            }

            // Process with Model C
            sendMessage(bot, chatId, "\n🟠 *Model C: Qwen2.5-7B-Instruct*")
            bot.sendChatAction(chatId = ChatId.fromId(chatId), action = ChatAction.TYPING)

            try {
                val modelCResponse = modelCClient.sendMessageWithMetrics(AiMessage("user", question, 0.7))
                sendMessage(bot, chatId, modelCResponse.content)
                sendMessage(bot, chatId, formatMetrics(modelCResponse.durationMs, modelCResponse.promptTokens, modelCResponse.completionTokens, modelCResponse.totalTokens))
            } catch (e: Exception) {
                sendMessage(bot, chatId, "❌ Error: ${e.message}")
            }

            // Summary
            sendMessage(bot, chatId, "\n━━━━━━━━━━━━━━━━━━━━━━━━\n✨ *Processing Complete!*\n\n" +
                    "All 3 HuggingFace models have responded to your question.")

        } catch (e: Exception) {
            sendMessage(bot, chatId, "❌ Sorry, an error occurred while processing your question: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun sendMessage(bot: Bot, chatId: Long, text: String) {
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = text,
            parseMode = ParseMode.MARKDOWN
        )
    }

    private fun formatMetrics(durationMs: Long, promptTokens: Long, completionTokens: Long, totalTokens: Long): String {
        val durationSeconds = durationMs / 1000.0
        return """
            📊 *Metrics:*
            ⏱️ Time: ${String.format("%.2f", durationSeconds)}s (${durationMs}ms)
            🔢 Tokens: $totalTokens total ($promptTokens prompt + $completionTokens completion)
        """.trimIndent()
    }
}
