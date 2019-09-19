package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.util.Sample

class ESavedMetric : Metric {
    private val sample = Sample()

    override val value: Double
        get() = sample.mean()

    override fun evaluate(sessions: List<Session>): Double {
        val fileSample = Sample()
        sessions.forEach {
            var rank = it.lookups.map { lookup -> Pair(lookup.suggestions, it.expectedText) }.indexOfFirst {
                (suggests, expectedText) -> suggests.isNotEmpty() && suggests.first().text == expectedText
            }
            if (rank < 0) {
                rank = it.lookups.size
            }
            val eSavedValue = (1.0 - rank.toDouble() / it.lookups.size)
            fileSample.add(eSavedValue)
            sample.add(eSavedValue)
        }

        return fileSample.mean()
    }

    override val name: String = "eSaved"

    override val format: (Double) -> String = { if (it.isNaN()) "—" else "%.3f".format(it) }
}