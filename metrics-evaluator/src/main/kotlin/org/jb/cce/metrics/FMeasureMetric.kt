package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.samples.FMeasureSample

class FMeasureMetric : Metric {
    private val precision = PrecisionMetric()
    private val recall = RecallMetric()

    override val sample = FMeasureSample(precision.sample, recall.sample)

    override fun evaluate(sessions: List<Session>): Double {
        val precisionValue = precision.evaluate(sessions)
        val recallValue = recall.evaluate(sessions)
        val fMeasureValue = 2 * precisionValue * recallValue / (precisionValue + recallValue)
        sample.add(fMeasureValue)
        return fMeasureValue
    }

    override val name: String = "F-Measure"
}