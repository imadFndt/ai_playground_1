package com.com

import com.com.ai.AiMessage
import com.com.ai.ClaudeResponse
import com.com.di.AppModule
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

const val MAX_TELEGRAM_MESSAGE_LENGTH = 1000

fun main() {
    val botToken = System.getenv("TELEGRAM_BOT_TOKEN")
        ?: throw IllegalStateException("TELEGRAM_BOT_TOKEN environment variable is not set")

    val bot = bot {
        token = botToken

        dispatch {
            command("start") {
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Hello! I'm an AI assistant powered by Claude. Send me any question and I'll do my best to help!"
                )
            }

            command("help") {
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = """
                        Available commands:
                        /start - Start the bot
                        /help - Show this help message

                        Just send me any text message and I'll respond using Claude AI!

                        Note: Maximum message length is $MAX_TELEGRAM_MESSAGE_LENGTH characters.
                    """.trimIndent()
                )
            }

            message {
                val text = message.text ?: return@message

                // Ignore commands
                if (text.startsWith("/")) {
                    return@message
                }

                // Check message length
                if (text.length > MAX_TELEGRAM_MESSAGE_LENGTH) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "Your message exceeds the maximum length of $MAX_TELEGRAM_MESSAGE_LENGTH characters. Please send a shorter message."
                    )
                    return@message
                }

                try {
                    // Send typing indicator
                    bot.sendChatAction(
                        chatId = ChatId.fromId(message.chat.id),
                        action = com.github.kotlintelegrambot.entities.ChatAction.TYPING
                    )

                    // Get AI response
                    val aiClient = AppModule.provideAiClient()
                    val jsonResponse = runBlocking {
                        aiClient.sendMessage(AiMessage("user", text))
                    }

                    // Parse JSON response
                    val claudeResponse = try {
                        val json = Json { ignoreUnknownKeys = true }
                        json.decodeFromString<ClaudeResponse>(jsonResponse)
                    } catch (e: Exception) {
                        bot.sendMessage(
                            chatId = ChatId.fromId(message.chat.id),
                            text = "Sorry, couldn't parse the response. Raw response: $jsonResponse"
                        )
                        return@message
                    }

                    // Format response for Telegram
                    val formattedResponse = """
                        📌 ${claudeResponse.title}

                        ${claudeResponse.answer}

                        💭 ${claudeResponse.randomBadThoughtAboutWriter}
                    """.trimIndent()

                    // Send response
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = formattedResponse
                    )
                } catch (e: Exception) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "Sorry, an error occurred while processing your request: ${e.message}"
                    )
                    e.printStackTrace()
                }
            }
        }
    }

    println("Telegram bot started! Waiting for messages...")
    bot.startPolling()
}
