package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.samples.AppendableSample

class ESavedMetric : Metric {
    override val sample = AppendableSample()

    override fun evaluate(sessions: List<Session>): Double {
        var eSavedSum = 0.0
        sessions.forEach {
            var rank = it.lookups.map { lookup -> Pair(lookup.suggests, it.expectedText) }.indexOfFirst {
                (suggests, expectedText) -> suggests.isNotEmpty() && suggests.first() == expectedText
            }
            if (rank < 0) {
                rank = it.lookups.size
            }
            eSavedSum += (1.0 - rank.toDouble() / it.lookups.size)
        }

        sample.add(eSavedSum, sessions.size.toLong())
        return eSavedSum / sessions.size
    }

    override val name: String = "eSaved"
}