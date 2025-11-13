package com.com.bot

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

data class ContextState(
    val enabled: Boolean = false,
    val contextSummary: String = "",
    val recentMessages: MutableList<String> = mutableListOf(),
    val messageCount: Int = 0
)

object ContextManager {
    private val logger = LoggerFactory.getLogger(ContextManager::class.java)
    private val contexts = ConcurrentHashMap<Long, ContextState>()

    @Synchronized
    fun enableContext(chatId: Long) {
        contexts[chatId] = ContextState(enabled = true)
        logger.info("Context collection enabled for chat: $chatId")
    }

    @Synchronized
    fun disableContext(chatId: Long) {
        contexts.remove(chatId)
        logger.info("Context collection disabled and cleared for chat: $chatId")
    }

    fun isContextEnabled(chatId: Long): Boolean {
        return contexts[chatId]?.enabled ?: false
    }

    fun getState(chatId: Long): ContextState? {
        return contexts[chatId]
    }

    @Synchronized
    fun updateState(chatId: Long, state: ContextState) {
        contexts[chatId] = state
    }

    @Synchronized
    fun addMessage(chatId: Long, role: String, content: String) {
        val state = contexts[chatId] ?: return
        if (!state.enabled) return

        val messageEntry = "$role: $content"
        val updatedMessages = state.recentMessages.toMutableList()
        updatedMessages.add(messageEntry)

        val newState = state.copy(
            recentMessages = updatedMessages,
            messageCount = state.messageCount + 1
        )
        contexts[chatId] = newState
    }

    fun shouldCompress(chatId: Long): Boolean {
        val state = contexts[chatId] ?: return false
        return state.messageCount >= 10
    }

    fun getContextForPrompt(chatId: Long): String? {
        val state = contexts[chatId]?.copy() ?: return null
        if (!state.enabled) return null

        val parts = mutableListOf<String>()

        if (state.contextSummary.isNotEmpty()) {
            parts.add("=== Previous Context Summary ===")
            parts.add(state.contextSummary)
            parts.add("")
        }

        if (state.recentMessages.isNotEmpty()) {
            parts.add("=== Recent Conversation ===")
            parts.addAll(state.recentMessages)
        }

        return if (parts.isNotEmpty()) {
            "=== CONVERSATION CONTEXT ===\n" + parts.joinToString("\n") + "\n=== END CONTEXT ==="
        } else {
            null
        }
    }

    @Synchronized
    fun compressContext(chatId: Long, compressedSummary: String) {
        val state = contexts[chatId] ?: return

        val messagesCompressed = state.recentMessages.size
        val summaryLength = compressedSummary.length

        contexts[chatId] = state.copy(
            contextSummary = compressedSummary,
            recentMessages = mutableListOf(),
            messageCount = 0
        )

        logger.info("Context compressed for chat: $chatId | Messages compressed: $messagesCompressed | Summary length: $summaryLength chars")
        logger.debug("Compressed summary for chat $chatId: ${compressedSummary.take(100)}...")
    }
}

object MetricsManager {
    private val logger = LoggerFactory.getLogger(MetricsManager::class.java)
    private val metricsEnabled = ConcurrentHashMap<Long, Boolean>()

    @Synchronized
    fun enableMetrics(chatId: Long) {
        metricsEnabled[chatId] = true
        logger.info("Metrics enabled for chat: $chatId")
    }

    @Synchronized
    fun disableMetrics(chatId: Long) {
        metricsEnabled[chatId] = false
        logger.info("Metrics disabled for chat: $chatId")
    }

    fun isMetricsEnabled(chatId: Long): Boolean {
        return metricsEnabled.getOrDefault(chatId, true) // Default to enabled
    }
}
