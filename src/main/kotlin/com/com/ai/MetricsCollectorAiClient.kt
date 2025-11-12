package com.com.ai

/**
 * Decorator for AiClient that delegates to MetricsCollector for tracking metrics
 */
class MetricsCollectorAiClient(
    private val delegate: AiClient,
    private val metricsCollector: MetricsCollector
) : AiClient {

    override suspend fun sendMessageWithMetrics(aiMessage: AiMessage): AiResponseWithMetrics {
        val response = delegate.sendMessageWithMetrics(aiMessage)
        metricsCollector.recordMetrics(response)
        return response
    }
}
