package com.com

import com.com.ai.AiMessage
import com.com.ai.ClaudeResponse
import com.com.ai.ConversationMessage
import com.com.ai.sendMessagePlainText
import com.com.bot.ContextManager
import com.com.bot.ConversationManager
import com.com.bot.FindTrackInteractor
import com.com.bot.MetricsManager
import com.com.di.AppModule
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

const val MAX_TELEGRAM_MESSAGE_LENGTH = 200000

private val logger = LoggerFactory.getLogger("TelegramBotApplication")

/**
 * Format comparison percentages for display
 * Shows difference vs average and median (50th percentile)
 */
fun formatComparison(vsAverage: Double, vsMedian: Double): String {
    val avgSymbol = when {
        vsAverage > 10 -> "🔴"
        vsAverage < -10 -> "🟢"
        else -> "⚪"
    }
    val medianSymbol = when {
        vsMedian > 10 -> "🔴"
        vsMedian < -10 -> "🟢"
        else -> "⚪"
    }

    val avgSign = if (vsAverage > 0) "+" else ""
    val medianSign = if (vsMedian > 0) "+" else ""

    return "($avgSymbol${avgSign}${String.format("%.0f", vsAverage)}% avg, $medianSymbol${medianSign}${String.format("%.0f", vsMedian)}% 50p)"
}

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
                        /json [question] - Get a JSON-formatted response with title, thought, and answer
                        /findTrack - Find music tracks based on year, genre, and region
                        /experts - Analyze a question using multiple AI approaches and compare results
                        /temperature - Compare answers across different temperature settings (0, 0.4, 0.9)
                        /differentModels - Process a question using 3 different HuggingFace models
                        /context [on|off] - Enable or disable conversation context collection
                        /metrics [on|off] - Enable or disable performance metrics display

                        Just send me any text message and I'll respond using Claude AI!

                        Note: Maximum message length is $MAX_TELEGRAM_MESSAGE_LENGTH characters.
                    """.trimIndent()
                )
            }

            command("json") {
                val question = message.text?.removePrefix("/json")?.trim()

                if (question.isNullOrBlank()) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "Please provide a question after the /json command.\nExample: /json What is the capital of France?"
                    )
                    return@command
                }

                if (question.length > MAX_TELEGRAM_MESSAGE_LENGTH) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "Question exceeds maximum length of $MAX_TELEGRAM_MESSAGE_LENGTH characters"
                    )
                    return@command
                }

                try {
                    bot.sendChatAction(
                        chatId = ChatId.fromId(message.chat.id),
                        action = com.github.kotlintelegrambot.entities.ChatAction.TYPING
                    )

                    val jsonResponseInteractor = AppModule.provideJsonResponseInteractor()
                    val jsonResponse = jsonResponseInteractor.getJsonResponse(AiMessage("user", question))

                    // Parse JSON response
                    val claudeResponse = try {
                        val json = Json { ignoreUnknownKeys = true }
                        json.decodeFromString<ClaudeResponse>(jsonResponse)
                    } catch (e: Exception) {
                        bot.sendMessage(
                            chatId = ChatId.fromId(message.chat.id),
                            text = "Sorry, couldn't parse the response. Raw response: $jsonResponse"
                        )
                        return@command
                    }

                    // Format response for Telegram
                    val formattedResponse = """
                        *${claudeResponse.title}*

                        _${claudeResponse.randomBadThoughtAboutWriter}_

                        ${claudeResponse.answer}
                    """.trimIndent()

                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = formattedResponse,
                        parseMode = ParseMode.MARKDOWN
                    )
                } catch (e: IllegalStateException) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "Error: ${e.message ?: "Invalid state while processing request"}"
                    )
                } catch (e: Exception) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "Error communicating with AI service: ${e.message ?: "Unknown error"}"
                    )
                }
            }

            command("findTrack") {
                val chatId = message.chat.id
                ConversationManager.startFindTrack(chatId)

                bot.sendChatAction(
                    chatId = ChatId.fromId(chatId),
                    action = com.github.kotlintelegrambot.entities.ChatAction.TYPING
                )

                try {
                    val aiClient = AppModule.provideAiClient()
                    val initialPrompt = """
                        You are starting a conversation to help someone find music tracks.
                        You need to gather exactly 3 pieces of information: year/era, genre, and region.
                        Ask an engaging opening question about ONE of these three topics.
                        Keep it natural and conversational.
                        Format your response as: QUESTION: [your question]
                    """.trimIndent()

                    val initialQuestion = aiClient.sendMessagePlainText(AiMessage("system", initialPrompt, temperature = 0.7))

                    val question = if (initialQuestion.startsWith("QUESTION:", ignoreCase = true)) {
                        initialQuestion.substringAfter(":", "").trim()
                    } else {
                        initialQuestion
                    }

                    val state = ConversationManager.getState(chatId)
                    state?.conversationHistory?.add("Assistant: $question")
                    state?.let { ConversationManager.updateState(chatId, it) }

                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = "🎵 Let's find some music tracks!\n\n$question"
                    )
                } catch (e: Exception) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = "🎵 Let's find some music tracks! What kind of music are you in the mood for?"
                    )
                }
            }

            command("experts") {
                val chatId = message.chat.id
                val questionText = message.text?.removePrefix("/experts")?.trim()

                if (questionText.isNullOrEmpty()) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = "Please provide a question after the /experts command.\n\nExample: /experts What is quantum computing?"
                    )
                    return@command
                }

                try {
                    val expertsInteractor = AppModule.provideExpertsInteractor()
                    expertsInteractor.processQuestion(bot, chatId, questionText)
                } catch (e: Exception) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = "❌ Sorry, an error occurred: ${e.message}"
                    )
                    e.printStackTrace()
                }
            }

            command("temperature") {
                val chatId = message.chat.id
                val questionText = message.text?.removePrefix("/temperature")?.trim()

                if (questionText.isNullOrEmpty()) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = "Please provide a question after the /temperature command.\n\nExample: /temperature What is machine learning?"
                    )
                    return@command
                }

                try {
                    val temperatureInteractor = AppModule.provideTemperatureInteractor()
                    temperatureInteractor.processQuestion(bot, chatId, questionText)
                } catch (e: Exception) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = "❌ Sorry, an error occurred: ${e.message}"
                    )
                    e.printStackTrace()
                }
            }

            command("differentModels") {
                val chatId = message.chat.id
                val questionText = message.text?.removePrefix("/differentModels")?.trim()

                if (questionText.isNullOrEmpty()) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = "Please provide a question after the /differentModels command.\n\nExample: /differentModels What is artificial intelligence?"
                    )
                    return@command
                }

                try {
                    val differentModelsInteractor = AppModule.provideDifferentModelsInteractor()
                    if (differentModelsInteractor == null) {
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = "⚠️ HuggingFace models are not configured. Please set the HUGGING_FACE_API_KEY environment variable."
                        )
                        return@command
                    }
                    differentModelsInteractor.processQuestion(bot, chatId, questionText)
                } catch (e: Exception) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
                        text = "❌ Sorry, an error occurred: ${e.message}"
                    )
                    e.printStackTrace()
                }
            }

            command("context") {
                val chatId = message.chat.id
                val argument = message.text?.removePrefix("/context")?.trim()?.lowercase()

                when (argument) {
                    "on" -> {
                        ContextManager.enableContext(chatId)
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = "✅ Context collection enabled!\n\nI will now collect conversation context and include it in the system prompt. Every 10 messages, the context will be automatically compressed into a summary."
                        )
                    }
                    "off" -> {
                        ContextManager.disableContext(chatId)
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = "✅ Context collection disabled and cleared for this conversation."
                        )
                    }
                    else -> {
                        val isEnabled = ContextManager.isContextEnabled(chatId)
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = """
                                📝 *Context Collection*

                                Status: ${if (isEnabled) "🟢 Enabled" else "🔴 Disabled"}

                                Usage:
                                /context on - Enable context collection
                                /context off - Disable and clear context

                                When enabled, I'll collect conversation history and include it as context in the system prompt. Every 10 messages, the context automatically compresses to a summary to keep the conversation efficient.
                            """.trimIndent(),
                            parseMode = ParseMode.MARKDOWN
                        )
                    }
                }
            }

            command("metrics") {
                val chatId = message.chat.id
                val argument = message.text?.removePrefix("/metrics")?.trim()?.lowercase()

                when (argument) {
                    "on" -> {
                        MetricsManager.enableMetrics(chatId)
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = "✅ Metrics display enabled!\n\nPerformance metrics will now be shown with AI responses."
                        )
                    }
                    "off" -> {
                        MetricsManager.disableMetrics(chatId)
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = "✅ Metrics display disabled!\n\nMetrics will no longer be shown with AI responses."
                        )
                    }
                    else -> {
                        val isEnabled = MetricsManager.isMetricsEnabled(chatId)
                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = """
                                📊 *Metrics Display*

                                Status: ${if (isEnabled) "🟢 Enabled" else "🔴 Disabled"}

                                Usage:
                                /metrics on - Enable metrics display
                                /metrics off - Disable metrics display

                                When enabled, performance metrics (response time, tokens used, etc.) will be shown with AI responses.
                            """.trimIndent(),
                            parseMode = ParseMode.MARKDOWN
                        )
                    }
                }
            }

            message {
                val text = message.text ?: return@message
                val chatId = message.chat.id

                // Ignore commands
                if (text.startsWith("/")) {
                    return@message
                }

                // Check if user is in a findTrack conversation
                if (ConversationManager.isInConversation(chatId)) {
                    val findTrackInteractor = AppModule.provideFindTrackInteractor()
                    findTrackInteractor.handleConversation(bot, chatId, text)
                    return@message
                }

                // Check message length
                if (text.length > MAX_TELEGRAM_MESSAGE_LENGTH) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(chatId),
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

                    val aiClient = AppModule.provideAiClient()

                    // Check if context compression is needed
                    if (ContextManager.shouldCompress(chatId)) {
                        logger.info("Starting context compression for chat: $chatId")
                        val compressionStartTime = System.currentTimeMillis()

                        val contextForSummary = ContextManager.getContextForPrompt(chatId) ?: ""

                        val summaryPrompt = """
                            Summarize the following conversation context concisely in 2-3 paragraphs.
                            Focus on key topics, important information, and the flow of the conversation:

                            $contextForSummary
                        """.trimIndent()

                        val summary = aiClient.sendMessagePlainText(
                            AiMessage("user", summaryPrompt, temperature = 0.3)
                        )

                        ContextManager.compressContext(chatId, summary)

                        val compressionDuration = System.currentTimeMillis() - compressionStartTime
                        logger.info("Context compression completed for chat: $chatId | Duration: ${compressionDuration}ms")
                    }

                    // Get AI response with metrics
                    val metricsCollector = AppModule.provideMetricsCollector()

                    // Prepare messages with context if enabled
                    val messages = mutableListOf<ConversationMessage>()

                    val contextPrompt = ContextManager.getContextForPrompt(chatId)
                    if (contextPrompt != null) {
                        messages.add(ConversationMessage("system", contextPrompt))
                    }

                    messages.add(ConversationMessage("user", text))

                    val responseWithMetrics = aiClient.sendMessageWithMetrics(
                        AiMessage(messages, temperature = 1.0)
                    )

                    // Track messages in context if enabled
                    ContextManager.addMessage(chatId, "user", text)
                    ContextManager.addMessage(chatId, "assistant", responseWithMetrics.content)

                    // Format response with metrics if enabled
                    val formattedResponse = if (MetricsManager.isMetricsEnabled(chatId)) {
                        // Calculate duration in seconds
                        val durationSeconds = responseWithMetrics.durationMs / 1000.0

                        // Get comparison with historical data
                        val comparison = metricsCollector.compareToStatistics(responseWithMetrics)

                        if (comparison != null) {
                            val stats = comparison.statistics
                            """
                            ${responseWithMetrics.content}

                            ━━━━━━━━━━━━━━━━━━━━
                            📊 *Current Metrics:*
                            ⏱️ Time: ${String.format("%.2f", durationSeconds)}s ${formatComparison(comparison.durationVsAverage, comparison.durationVsMedian)}
                            📥 Prompt tokens: ${responseWithMetrics.promptTokens} ${formatComparison(comparison.promptTokensVsAverage, comparison.promptTokensVsMedian)}
                            📤 Completion tokens: ${responseWithMetrics.completionTokens} ${formatComparison(comparison.completionTokensVsAverage, comparison.completionTokensVsMedian)}
                            📊 Total tokens: ${responseWithMetrics.totalTokens} ${formatComparison(comparison.totalTokensVsAverage, comparison.totalTokensVsMedian)}

                            📈 *Historical Stats (${stats.totalRequests} requests):*
                            ⏱️ Avg: ${String.format("%.2f", stats.averageDurationMs / 1000.0)}s | Median: ${String.format("%.2f", stats.medianDurationMs / 1000.0)}s
                            📊 Avg tokens: ${String.format("%.0f", stats.averageTotalTokens)} | Median: ${String.format("%.0f", stats.medianTotalTokens)}
                            """.trimIndent()
                        } else {
                            """
                            ${responseWithMetrics.content}

                            ━━━━━━━━━━━━━━━━━━━━
                            📊 *Metrics:*
                            ⏱️ Time: ${String.format("%.2f", durationSeconds)}s
                            📥 Prompt tokens: ${responseWithMetrics.promptTokens}
                            📤 Completion tokens: ${responseWithMetrics.completionTokens}
                            📊 Total tokens: ${responseWithMetrics.totalTokens}

                            ℹ️ First request - no historical data yet
                            """.trimIndent()
                        }
                    } else {
                        // Metrics disabled - just return the content
                        responseWithMetrics.content
                    }

                    // Send response
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = formattedResponse,
                        parseMode = ParseMode.MARKDOWN
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
