package org.jb.cce.metrics

import org.jb.cce.Session
import java.io.PrintStream

class MetricsEvaluator {

    private val metrics = mutableListOf<Metric>()

    fun registerDefaultMetrics() {
        registerMetric(PrecisionMetric())
        registerMetric(RecallMetric())
        registerMetric(FMeasureMetric())
        registerMetric(MeanReciprocalRankMetric())
//        registerMetric(ESavedMetric())
    }

    fun registerMetric(metric: Metric) {
        metrics.add(metric)
    }

    fun evaluate(sessions: List<Session>, fileName: String,  out: PrintStream) {
        out.println("Completion quality evaluation for file $fileName:")

        if (metrics.isNullOrEmpty()) {
            out.println("No metrics to evaluate")
        }

        for (metric in metrics) {
            out.println("${metric.name} Metric value = ${metric.evaluate(sessions)}")
        }
    }

    fun printResult(out: PrintStream) {
        out.println("Completion quality evaluation results:")

        if (metrics.isNullOrEmpty()) {
            out.println("No evaluated metrics")
        }

        for (metric in metrics) {
            out.println("${metric.name} Metric value = ${metric.value}")
        }
    }
}