package org.jb.cce.metrics

import org.jb.cce.Session

class FMeasureMetric : Metric {
    private val precision = PrecisionMetric()
    private val recall = RecallMetric()

    override val value: Double
        get() = 2 * precision.value * recall.value / (precision.value + recall.value)

    override fun evaluate(sessions: List<Session>): Double {
        val precision = precision.evaluate(sessions)
        val recall = recall.evaluate(sessions)
        return 2 * precision * recall / (precision + recall)
    }

    override val name: String = "F-Measure"
}