package com.com.bot

import com.com.ai.AiClient
import com.com.ai.AiMessage
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode

class ExpertsInteractor(
    private val claudeClient: AiClient,
    private val yandexClient: AiClient?
) {

    suspend fun processQuestion(bot: Bot, chatId: Long, question: String) {
        bot.sendChatAction(chatId = ChatId.fromId(chatId), action = ChatAction.TYPING)

        try {
            // Check if Yandex is available
            if (yandexClient == null) {
                sendMessage(bot, chatId, "ℹ️ *Note:* Yandex GPT is not configured. Only Claude will be used.\n")
            }

            // Step 1: Create validation prompt with AI
            sendStep(bot, chatId, "🔍 *Step 1: Creating Answer Validation Prompt with AI*")

            val validationPrompt = claudeClient.sendMessagePlainText(AiMessage("user", """
                Create a validation prompt for evaluating answers to the following question.
                Do NOT decide right answer from question, listen to arguments from answer.
                The validation prompt should check for:
                1. Accuracy and correctness
                2. Completeness of the answer
                3. Clarity and structure
                4. Relevance to the question

                Question: $question

                IMPORTANT: Answer in the same language as the question.
                Provide only the validation prompt text.
            """.trimIndent(), temperature = 0.3))

            sendMessage(bot, chatId, "*Generated Validation Prompt:*\n\n$validationPrompt")

            // Step 2: Simple processing
            sendStep(bot, chatId, "\n🤖 *Step 2: Simple Question Processing*")
            val simplePrompt = "IMPORTANT: Answer in the same language as the question.\n\nQuestion: $question"
            val (claudeSimple, yandexSimple) = processWithValidation(
                bot, chatId, validationPrompt, simplePrompt, 0.7, "Answer", question
            )

            // Step 3: Step-by-step processing
            sendStep(bot, chatId, "\n📋 *Step 3: Step-by-Step Analysis*")
            val stepPrompt = """
                IMPORTANT: Answer in the same language as the question.

                Analyze the following question step by step:
                1. Identify the key components of the question
                2. Break down what information is needed
                3. Provide a structured answer addressing each component

                Question: $question
            """.trimIndent()

            val (claudeStep, yandexStep) = processWithValidation(
                bot, chatId, validationPrompt, stepPrompt, 0.5, "Step-by-Step", question
            )

            // Step 4: Expert group approach
            sendStep(bot, chatId, "\n👥 *Step 4: Expert Group Analysis*")
            val expertPrompt = """
                IMPORTANT: Answer in the same language as the question.

                Imagine you are assembling a group of experts to answer this question.

                1. First, identify what types of experts would be needed (e.g., domain specialists, researchers, analysts)
                2. List 3-4 specific expert roles
                3. For each expert, provide their perspective on the question
                4. Finally, synthesize their insights into a comprehensive answer

                Question: $question
            """.trimIndent()

            val (claudeExpert, yandexExpert) = processWithValidation(
                bot, chatId, validationPrompt, expertPrompt, 0.7, "Expert Group", question
            )

            // Final summary
            sendMessage(bot, chatId, "\n✨ *Analysis Complete!*\n\nAll approaches have been processed and validated successfully.")

        } catch (e: Exception) {
            sendMessage(bot, chatId, "❌ Sorry, an error occurred while processing your question: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun processWithValidation(
        bot: Bot,
        chatId: Long,
        validationPrompt: String,
        prompt: String,
        temperature: Double,
        label: String,
        originalQuestion: String
    ): Pair<String, String?> {
        // Get and send Claude answer
        val claudeAnswer = claudeClient.sendMessagePlainText(AiMessage("user", prompt, temperature))
        sendMessage(bot, chatId, "*Claude $label:*\n$claudeAnswer")

        // Validate Claude answer
        val claudeValidation = claudeClient.sendMessagePlainText(
            AiMessage("user", "$validationPrompt\n\nAnswer to validate:\n$claudeAnswer", 0.3)
        )
        sendMessage(bot, chatId, "*Claude Answer Validation:*\n$claudeValidation")

        // Process Yandex if available
        val yandexAnswer = if (yandexClient != null) {
            // Get and send Yandex answer
            val yandexPrompt = "IMPORTANT: Keep your answer concise and under 300 words.\n\n$prompt"
            val answer = yandexClient.sendMessagePlainText(AiMessage("user", yandexPrompt, temperature))
            sendMessage(bot, chatId, "*Yandex $label:*\n$answer")

            // Validate Yandex answer
            val yandexValidationPrompt = "IMPORTANT: Keep validation concise, max 150 words.\n\n$validationPrompt\n\nAnswer to validate:\n$answer"
            val validation = yandexClient.sendMessagePlainText(
                AiMessage("user", yandexValidationPrompt, 0.3)
            )
            sendMessage(bot, chatId, "*Yandex Answer Validation:*\n$validation")

            answer
        } else {
            null
        }

        // Compare answers if Yandex is available
        if (yandexAnswer != null) {
            val comparison = claudeClient.sendMessagePlainText(AiMessage("user", """
                IMPORTANT: Answer in the same language as the original question.

                Compare these two answers to the question: "$originalQuestion"

                Claude's answer:
                $claudeAnswer

                Yandex's answer:
                $yandexAnswer

                Provide a brief comparison highlighting strengths and differences.
            """.trimIndent(), 0.5))

            sendMessage(bot, chatId, "📊 *Comparison:*\n$comparison")
        }

        return Pair(claudeAnswer, yandexAnswer)
    }

    private fun sendStep(bot: Bot, chatId: Long, text: String) {
        sendMessage(bot, chatId, text)
    }

    private fun sendMessage(bot: Bot, chatId: Long, text: String) {
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = text,
            parseMode = ParseMode.MARKDOWN
        )
    }
}
