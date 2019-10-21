package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.util.Sample

class SessionsCountMetric : Metric {
    private val sample = Sample()

    override val value: Double
        get() = sample.sum()

    override fun evaluate(sessions: List<Session>): Double {
        sample.add(sessions.size.toDouble())
        return sessions.size.toDouble()
    }

    override val name: String = "Sessions"

    override val valueType = MetricValueType.INT
}