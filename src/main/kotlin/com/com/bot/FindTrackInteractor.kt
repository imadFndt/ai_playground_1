package com.com.bot

import com.com.ai.AiClient
import com.com.ai.AiMessage
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import kotlinx.coroutines.runBlocking

class FindTrackInteractor(private val aiClient: AiClient) {

    fun handleConversation(bot: Bot, chatId: Long, userMessage: String) {
        val state = ConversationManager.getState(chatId) ?: return

        bot.sendChatAction(
            chatId = ChatId.fromId(chatId),
            action = ChatAction.TYPING
        )

        try {
            // Add user message to conversation history
            state.conversationHistory.add("User: $userMessage")

            // Build conversation context
            val conversationContext = state.conversationHistory.joinToString("\n")

            // Ask AI to decide next step
            val systemPrompt = """
                You are helping find music tracks for a user. You need to gather exactly 3 pieces of information:
                1. Year or era (e.g., "1980s", "2010", "90s")
                2. Genre (e.g., "rock", "hip-hop", "jazz")
                3. Geographic region (e.g., "USA", "UK", "Japan", "Latin America")

                Review the conversation history and decide:

                1. If you have ALL THREE pieces of information (year/era, genre, and region), respond with:
                   READY: [then provide 3 specific track recommendations with artist names]

                2. If you are MISSING any of these three pieces, ask ONE specific question about the missing information. Respond with:
                   QUESTION: [your question]

                Be conversational and natural. Only ask about year/era, genre, or region - nothing else.

                Conversation so far:
                $conversationContext
            """.trimIndent()

            val aiResponse = runBlocking {
                aiClient.sendMessagePlainText(AiMessage("system", systemPrompt, temperature = 0.7))
            }

            // Add AI response to history
            state.conversationHistory.add("Assistant: $aiResponse")

            when {
                aiResponse.startsWith("READY:", ignoreCase = true) -> {
                    // AI has enough info, provide recommendations and end conversation
                    val recommendations = aiResponse.substringAfter(":", "").trim()
                    ConversationManager.clearState(chatId)

                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = "🎵 *Music Recommendations:*\n\n$recommendations",
                        parseMode = ParseMode.MARKDOWN
                    )
                }
                aiResponse.startsWith("QUESTION:", ignoreCase = true) -> {
                    // AI needs more info, ask the question
                    val question = aiResponse.substringAfter(":", "").trim()
                    ConversationManager.updateState(chatId, state)

                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = question
                    )
                }
                else -> {
                    // Fallback: treat as question
                    ConversationManager.updateState(chatId, state)

                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = aiResponse
                    )
                }
            }
        } catch (e: Exception) {
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "Sorry, an error occurred while processing your request: ${e.message}"
            )
            e.printStackTrace()
        }
    }
}
