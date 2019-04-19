package org.jb.cce.metrics

import org.jb.cce.Session

object FMeasureMetric : Metric {
    override fun clear() {
        PrecisionMetric.clear()
        RecallMetric.clear()
    }

    override val aggregatedValue: Double
        get() = 2 * PrecisionMetric.aggregatedValue * RecallMetric.aggregatedValue / (PrecisionMetric.aggregatedValue + RecallMetric.aggregatedValue)

    override fun evaluate(sessions: List<Session>, update: Boolean): Double {
        val precision = PrecisionMetric.evaluate(sessions, false)
        val recall = RecallMetric.evaluate(sessions, false)
        return 2 * precision * recall / (precision + recall)
    }

    override val name: String = "F-Measure"
}