package org.jb.cce.metrics

import org.jb.cce.Session
import java.io.PrintStream

class MetricsEvaluator {

    private val metrics = mutableListOf<Metric>()

    fun registerDefaultMetrics() {
        registerMetric(PrecisionMetric)
        registerMetric(RecallMetric)
        registerMetric(FMeasureMetric)
        registerMetric(ESavedMetric)
        registerMetric(MeanReciprocalRankMetric)
    }

    fun registerMetric(metric: Metric) {
        metrics.add(metric)
    }

    fun evaluate(sessions: List<Session>, out: PrintStream) {
        out.println("Completion quality evaluation for project files in selected directory:")

        if (metrics.isNullOrEmpty()) {
            out.println("No metrics to evaluate")
        }

        for (metric in metrics) {
            out.println("${metric.name} Metric value = ${metric.evaluate(sessions)}")
        }
    }
}