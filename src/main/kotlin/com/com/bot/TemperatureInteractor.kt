package com.com.bot

import com.com.ai.AiClient
import com.com.ai.AiMessage
import com.com.ai.sendMessagePlainText
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode

class TemperatureInteractor(
    private val aiClient: AiClient
) {

    suspend fun processQuestion(bot: Bot, chatId: Long, question: String) {
        bot.sendChatAction(chatId = ChatId.fromId(chatId), action = ChatAction.TYPING)

        try {
            sendMessage(bot, chatId, "🌡️ *Temperature Comparison Analysis*\n\nProcessing question with different temperature settings...\n")

            val temperatures = listOf(0.0, 0.4, 0.9)
            val answers = mutableListOf<Pair<Double, String>>()

            // Get answers for each temperature
            for (temp in temperatures) {
                sendMessage(bot, chatId, "📊 *Temperature: $temp*")

                val prompt = "IMPORTANT: Answer in the same language as the question.\n\nQuestion: $question"
                val answer = aiClient.sendMessagePlainText(AiMessage("user", prompt, temp))

                sendMessage(bot, chatId, answer)
                answers.add(Pair(temp, answer))
            }

            // Compare all answers
            sendMessage(bot, chatId, "\n🔍 *Comparison Analysis*")

            val comparisonPrompt = """
                IMPORTANT: Answer in the same language as the original question.

                Analyze and compare these three answers to the question: "$question"

                The answers were generated with different creativity levels:

                Answer 1 (Temperature 0.0 - Most deterministic, focused):
                ${answers[0].second}

                Answer 2 (Temperature 0.4 - Balanced):
                ${answers[1].second}

                Answer 3 (Temperature 0.9 - Most creative, diverse):
                ${answers[2].second}

                Compare the answers and discuss:
                1. How temperature affected the response style and content
                2. Which temperature level produced the most appropriate answer for this question
                3. Key differences in approach, detail, and creativity
                4. Recommendations for optimal temperature for similar questions
            """.trimIndent()

            val comparison = aiClient.sendMessagePlainText(AiMessage("user", comparisonPrompt, 0.5))
            sendMessage(bot, chatId, "📝 *Analysis:*\n$comparison")

            sendMessage(bot, chatId, "\n✨ *Temperature comparison complete!*")

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
}
