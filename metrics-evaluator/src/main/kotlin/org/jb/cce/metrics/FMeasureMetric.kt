package org.jb.cce.metrics

import org.jb.cce.Session

object FMeasureMetric : Metric {
    override fun evaluate(sessions: List<Session>): Double {
        val precision = PrecisionMetric.evaluate(sessions)
        val recall = RecallMetric.evaluate(sessions)
        return 2 * precision * recall / (precision + recall)
    }

    override val name: String = "F-Measure"
}