package com.com.ai

import kotlinx.serialization.Serializable

@Serializable
data class ClaudeResponse(
    val title: String,
    val randomBadThoughtAboutWriter: String,
    val answer: String
)
