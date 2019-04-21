package org.jb.cce.metrics

import org.jb.cce.Session

class ESavedMetric : Metric {

    override val value: Double
        get() = TODO("not implemented")

    override fun evaluate(sessions: List<Session>): Double {
        TODO("not implemented")
    }

    override val name: String = "eSaved"
}