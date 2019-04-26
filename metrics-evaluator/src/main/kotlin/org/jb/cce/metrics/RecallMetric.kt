package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.util.Sample
import java.util.stream.Collectors

class RecallMetric : Metric {
    private val sample = Sample()

    override val value: Double
        get() = sample.mean()

    override fun evaluate(sessions: List<Session>): Double {
        val listOfCompletions = sessions.stream()
                .flatMap { session -> session.lookups.map { lookup -> Pair(lookup.suggests, session.expectedText) }.stream() }
                .collect(Collectors.toList())

        var recommendationsMadeCount = 0
        listOfCompletions.stream()
                .forEach { (suggests, expectedText) ->
                    if (suggests.contains(expectedText)) {
                        sample.add(1.0)
                        recommendationsMadeCount++
                    } else
                        sample.add(0.0)
                }

        return recommendationsMadeCount.toDouble() / listOfCompletions.size
    }

    override val name: String = "Recall"
}