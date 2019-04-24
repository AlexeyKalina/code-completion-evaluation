package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.samples.AppendableSample
import java.util.stream.Collectors

class PrecisionMetric : Metric {
    override val sample = AppendableSample()

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
            }
        }

        sample.add(relevantRecommendationsCount.toDouble(), recommendationsMadeCount)
        return relevantRecommendationsCount.toDouble() / recommendationsMadeCount
    }

    override val name: String = "Precision"
}