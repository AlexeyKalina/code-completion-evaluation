package org.jb.cce.metrics

import org.jb.cce.Session

interface Metric {
    fun evaluate(sessions: List<Session>): Double
    val value: Double
    val name: String
    val format: (Double) -> String

    companion object {
        val DEFAULT_DOUBLE_VALUE_FORMAT = { value: Double ->
            if (value.isNaN()) "—" else "%.3f".format(value)
        }
    }
}