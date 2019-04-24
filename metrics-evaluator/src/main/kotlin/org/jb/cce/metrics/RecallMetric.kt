package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.samples.AppendableSample
import java.util.stream.Collectors

class RecallMetric : Metric {
    override val sample = AppendableSample()

    override fun evaluate(sessions: List<Session>): Double {
        val listOfCompletions = sessions.stream()
                .flatMap { session -> session.lookups.map { lookup -> Pair(lookup.suggests, session.expectedText) }.stream() }
                .collect(Collectors.toList())
        val recommendationsMadeCount = listOfCompletions.stream()
                .filter { (suggests, expectedText) -> suggests.contains(expectedText) }
                .count()

        sample.add(recommendationsMadeCount.toDouble(), listOfCompletions.size.toLong())
        return recommendationsMadeCount.toDouble() / listOfCompletions.size
    }

    override val name: String = "Recall"
}