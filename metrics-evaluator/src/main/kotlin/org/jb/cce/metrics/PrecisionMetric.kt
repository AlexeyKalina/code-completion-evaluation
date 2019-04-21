package org.jb.cce.metrics

import org.jb.cce.Session
import java.util.stream.Collectors

class PrecisionMetric : Metric {
    private var totalCount = 0L
    private var relevantCount = 0L

    override val value: Double
        get() = if (totalCount == 0L) 0.0 else relevantCount.toDouble() / totalCount.toDouble()

    override fun evaluate(sessions: List<Session>): Double {
        // top3
        val listOfCompletions = sessions.stream()
                .map { compl -> compl.lookups }
                .collect(Collectors.toList())
        val recommendationsMadeCount = listOfCompletions.stream()
                .filter { l -> !l.isEmpty() }
                .count()

        val listOfRealCompletions = sessions.stream()
                .map { compl -> compl.completion }
                .collect(Collectors.toList())

        assert(listOfCompletions.size == listOfRealCompletions.size)
        var relevantRecommendationsCount = 0

        for ((completionIndex, realCompletion) in listOfRealCompletions.withIndex()) {
            val indexOfNecessaryCompletion = listOfCompletions[completionIndex].indexOf(realCompletion)
            if (indexOfNecessaryCompletion in 0..3) {
                relevantRecommendationsCount++
            }
        }

        totalCount += recommendationsMadeCount
        relevantCount += relevantRecommendationsCount
        return relevantRecommendationsCount.toDouble() / recommendationsMadeCount
    }

    override val name: String = "Precision"
}