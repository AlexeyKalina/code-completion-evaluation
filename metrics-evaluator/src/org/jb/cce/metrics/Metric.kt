package org.jb.cce.metrics

import org.jb.cce.Session

interface Metric {
    fun evaluate(sessions: List<Session>): Double
    val value: Double
    val name: String
    val valueType: MetricValueType
}