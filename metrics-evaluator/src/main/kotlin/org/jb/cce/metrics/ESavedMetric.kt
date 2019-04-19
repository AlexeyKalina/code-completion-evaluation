package org.jb.cce.metrics

import org.jb.cce.Session

object ESavedMetric : Metric {
    override fun clear() {
        TODO("not implemented")
    }

    override val aggregatedValue: Double
        get() = TODO("not implemented")

    override fun evaluate(sessions: List<Session>, update: Boolean): Double {
        TODO("not implemented")
    }

    override val name: String = "eSaved"
}