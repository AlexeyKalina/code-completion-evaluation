package org.jb.cce.metrics

import org.jb.cce.Session
import java.io.PrintStream

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
        registerMetric(PrecisionMetric())
        registerMetric(RecallMetric())
        registerMetric(FMeasureMetric())
        registerMetric(MeanReciprocalRankMetric())
        registerMetric(ESavedMetric())
    }

    fun registerMetric(metric: Metric) {
        metrics.add(metric)
    }

    fun evaluate(sessions: List<Session>, out: PrintStream) {
        if (metrics.isNullOrEmpty()) {
            out.println("No metrics to evaluate")
        }

        for (metric in metrics) {
            out.println("${metric.name} Metric value = ${metric.evaluate(sessions)}")
        }
    }

    fun printResult(out: PrintStream) {
        if (metrics.isNullOrEmpty()) {
            out.println("No evaluated metrics")
        }

        for (metric in metrics) {
            out.println("${metric.name} Metric value = ${metric.value}")
        }
    }
}