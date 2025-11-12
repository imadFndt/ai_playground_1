package com.com.ai

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Collects and analyzes metrics from AI responses
 */
class MetricsCollector {

    private val metricsHistory = ConcurrentLinkedQueue<AiResponseWithMetrics>()

    /**
     * Record a response's metrics
     */
    fun recordMetrics(response: AiResponseWithMetrics) {
        metricsHistory.add(response)
    }

    /**
     * Get statistics about collected metrics
     */
    fun getStatistics(): MetricsStatistics? {
        val metrics = metricsHistory.toList()
        if (metrics.isEmpty()) {
            return null
        }

        val durations = metrics.map { it.durationMs }
        val promptTokens = metrics.map { it.promptTokens }
        val completionTokens = metrics.map { it.completionTokens }
        val totalTokens = metrics.map { it.totalTokens }

        return MetricsStatistics(
            totalRequests = metrics.size,
            averageDurationMs = durations.average(),
            medianDurationMs = durations.sorted()[durations.size / 2].toDouble(),
            averagePromptTokens = promptTokens.average(),
            medianPromptTokens = promptTokens.sorted()[promptTokens.size / 2].toDouble(),
            averageCompletionTokens = completionTokens.average(),
            medianCompletionTokens = completionTokens.sorted()[completionTokens.size / 2].toDouble(),
            averageTotalTokens = totalTokens.average(),
            medianTotalTokens = totalTokens.sorted()[totalTokens.size / 2].toDouble()
        )
    }

    /**
     * Compare current metrics to historical statistics
     */
    fun compareToStatistics(current: AiResponseWithMetrics): MetricsComparison? {
        val stats = getStatistics() ?: return null

        return MetricsComparison(
            durationVsAverage = calculatePercentageDiff(current.durationMs.toDouble(), stats.averageDurationMs),
            durationVsMedian = calculatePercentageDiff(current.durationMs.toDouble(), stats.medianDurationMs),
            promptTokensVsAverage = calculatePercentageDiff(current.promptTokens.toDouble(), stats.averagePromptTokens),
            promptTokensVsMedian = calculatePercentageDiff(current.promptTokens.toDouble(), stats.medianPromptTokens),
            completionTokensVsAverage = calculatePercentageDiff(current.completionTokens.toDouble(), stats.averageCompletionTokens),
            completionTokensVsMedian = calculatePercentageDiff(current.completionTokens.toDouble(), stats.medianCompletionTokens),
            totalTokensVsAverage = calculatePercentageDiff(current.totalTokens.toDouble(), stats.averageTotalTokens),
            totalTokensVsMedian = calculatePercentageDiff(current.totalTokens.toDouble(), stats.medianTotalTokens),
            statistics = stats
        )
    }

    private fun calculatePercentageDiff(current: Double, baseline: Double): Double {
        if (baseline == 0.0) return 0.0
        return ((current - baseline) / baseline) * 100.0
    }

    /**
     * Clear all collected metrics
     */
    fun clearMetrics() {
        metricsHistory.clear()
    }

    /**
     * Get total number of recorded requests
     */
    fun getTotalRequests(): Int = metricsHistory.size
}

data class MetricsStatistics(
    val totalRequests: Int,
    val averageDurationMs: Double,
    val medianDurationMs: Double,
    val averagePromptTokens: Double,
    val medianPromptTokens: Double,
    val averageCompletionTokens: Double,
    val medianCompletionTokens: Double,
    val averageTotalTokens: Double,
    val medianTotalTokens: Double
)

data class MetricsComparison(
    val durationVsAverage: Double,
    val durationVsMedian: Double,
    val promptTokensVsAverage: Double,
    val promptTokensVsMedian: Double,
    val completionTokensVsAverage: Double,
    val completionTokensVsMedian: Double,
    val totalTokensVsAverage: Double,
    val totalTokensVsMedian: Double,
    val statistics: MetricsStatistics
)
