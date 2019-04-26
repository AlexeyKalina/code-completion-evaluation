package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.util.Sample

class MeanReciprocalRankMetric : Metric {
    private val sample = Sample()

    override val value: Double
        get() = sample.mean()

    override fun evaluate(sessions: List<Session>): Double {
        var rankSum = 0.0
        var totalLookupsCount = 0

        sessions.forEach {
            totalLookupsCount += it.lookups.size
            it.lookups.map { lookup ->  Pair(lookup.suggests, it.expectedText) }.forEach { (suggests, expectedText) ->
                val rank = suggests.indexOf(expectedText) + 1
                if (rank > 0) {
                    rankSum += 1.0 / rank
                    sample.add(1.0 / rank)
                } else
                    sample.add(0.0)
            }
        }

        return rankSum / totalLookupsCount
    }

    override val name: String =  "Mean Reciprocal Rank"
}