package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.util.Sample
import java.util.stream.Collectors

class PrecisionMetric : Metric {
    private val sample = Sample()

    override val value: Double
        get() = sample.mean()

    override fun evaluate(sessions: List<Session>): Double {
        // top3
        val listOfCompletions = sessions.stream()
                .flatMap { session -> session.lookups.map { lookup -> Pair(lookup.suggests, session.expectedText) }.stream() }
                .collect(Collectors.toList())
        val recommendationsMadeCount = listOfCompletions.stream()
                .filter { l -> !l.first.isEmpty() }
                .count()

        var relevantRecommendationsCount = 0
        for (completion in listOfCompletions) {
            val indexOfNecessaryCompletion = completion.first.indexOf(completion.second)
            if (indexOfNecessaryCompletion in 0..3) {
                relevantRecommendationsCount++
                sample.add(1.0)
            } else
                sample.add(0.0)
        }

        return relevantRecommendationsCount.toDouble() / recommendationsMadeCount
    }

    override val name: String = "Precision"
}