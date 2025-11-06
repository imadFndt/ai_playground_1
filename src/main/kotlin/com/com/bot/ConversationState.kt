package com.com.bot

data class FindTrackState(
    val conversationHistory: MutableList<String> = mutableListOf()
)

object ConversationManager {
    private val conversations = mutableMapOf<Long, FindTrackState>()

    fun startFindTrack(chatId: Long) {
        conversations[chatId] = FindTrackState()
    }

    fun getState(chatId: Long): FindTrackState? {
        return conversations[chatId]
    }

    fun updateState(chatId: Long, state: FindTrackState) {
        conversations[chatId] = state
    }

    fun clearState(chatId: Long) {
        conversations.remove(chatId)
    }

    fun isInConversation(chatId: Long): Boolean {
        return conversations.containsKey(chatId)
    }
}
