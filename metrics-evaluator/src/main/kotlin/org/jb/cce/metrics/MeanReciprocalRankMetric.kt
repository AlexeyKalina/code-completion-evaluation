package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.samples.AppendableSample

class MeanReciprocalRankMetric : Metric {
    override val sample = AppendableSample()

    override fun evaluate(sessions: List<Session>): Double {
        var rankSum = 0.0
        var totalLookupsCount = 0

        sessions.forEach {
            totalLookupsCount += it.lookups.size
            it.lookups.map { lookup ->  Pair(lookup.suggests, it.expectedText) }.forEach { (suggests, expectedText) ->
                val rank = suggests.indexOf(expectedText) + 1
                if (rank > 0) {
                    rankSum += 1.0 / rank
                }
            }
        }

        sample.add(rankSum, totalLookupsCount.toLong())
        return rankSum / totalLookupsCount
    }

    override val name: String =  "Mean Reciprocal Rank"
}