package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.util.Sample
import java.util.stream.Collectors

class AverageSelectedPositionMetric : Metric {
    private val sample = Sample()

    override val value: Double
        get() = sample.mean()

    override fun evaluate(sessions: List<Session>): Double {
        val listOfCompletions = sessions.stream()
                .map { session -> Pair(session.lookups.last().suggestions, session.expectedText) }
                .collect(Collectors.toList())

        val fileSample = Sample()
        listOfCompletions.stream()
                .forEach { (suggests, expectedText) ->
                    val position = suggests.indexOfFirst { it.text == expectedText }
                    if (position != -1) {
                        val positionToAdd = (position + 1).toDouble()
                        fileSample.add(positionToAdd)
                        sample.add(positionToAdd)
                    }
                }

        return fileSample.mean()
    }

    override val name: String = "Average Selected Position"
}