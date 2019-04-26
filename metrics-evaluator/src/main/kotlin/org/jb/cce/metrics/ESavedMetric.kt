package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.util.Sample

class ESavedMetric : Metric {
    private val sample = Sample()

    override val value: Double
        get() = sample.mean()

    override fun evaluate(sessions: List<Session>): Double {
        var eSavedSum = 0.0
        sessions.forEach {
            var rank = it.lookups.map { lookup -> Pair(lookup.suggests, it.expectedText) }.indexOfFirst {
                (suggests, expectedText) -> suggests.isNotEmpty() && suggests.first() == expectedText
            }
            if (rank < 0) {
                rank = it.lookups.size
            }
            val eSavedValue = (1.0 - rank.toDouble() / it.lookups.size)
            eSavedSum += eSavedValue
            sample.add(eSavedValue)
        }

        return eSavedSum / sessions.size
    }

    override val name: String = "eSaved"
}