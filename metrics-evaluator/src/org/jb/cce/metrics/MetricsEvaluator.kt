package org.jb.cce.metrics

import org.jb.cce.Session

class MetricsEvaluator private constructor() {
    companion object {
        fun withDefaultMetrics(): MetricsEvaluator {
            val evaluator = MetricsEvaluator()
            evaluator.registerDefaultMetrics()
            return evaluator
        }
    }

    private val metrics = mutableListOf<Metric>()

    fun registerDefaultMetrics() {
        registerMetric(PrecisionMetric(1))
        registerMetric(PrecisionMetric(5))
        registerMetric(RecallMetric())
        registerMetric(ESavedMetric())
        registerMetric(MeanLatencyMetric())
        registerMetric(MaxLatencyMetric())
        registerMetric(AverageSelectedPositionMetric())
    }

    fun registerMetric(metric: Metric) {
        metrics.add(metric)
    }

    fun evaluate(sessions: List<Session>): List<MetricInfo> {
        return metrics.map { MetricInfo(it.name, it.evaluate(sessions)) }
    }

    fun result(): List<MetricInfo> {
        return metrics.map { MetricInfo(it.name, it.value) }
    }
}