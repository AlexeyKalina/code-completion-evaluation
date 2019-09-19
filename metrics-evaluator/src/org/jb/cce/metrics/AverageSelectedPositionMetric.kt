package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.util.Sample

class AverageSelectedPositionMetric : Metric {
    private val sample = Sample()

    override val value: Double
        get() = sample.mean()

    override fun evaluate(sessions: List<Session>): Double {
        val completions = sessions.map { session -> Pair(session.lookups.last().suggestions, session.expectedText) }

        val fileSample = Sample()
        completions.forEach { (suggests, expectedText) ->
            val position = suggests.indexOfFirst { it.text == expectedText }
            if (position != -1) {
                fileSample.add(position.toDouble())
                sample.add(position.toDouble())
            }
        }

        return fileSample.mean()
    }

    override val name: String = "Average Selected Position"

    override val format: (Double) -> String = { if (it.isNaN()) "—" else "%.3f".format(it) }
}