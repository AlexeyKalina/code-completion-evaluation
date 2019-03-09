package org.jb.cce.metrics

import org.jb.cce.Session

class MeanReciprocalRankMetricsEvaluator {
    companion object {
        fun evaluate(sessions: List<Session>): Double {
            var rankSum = 0.0
            var totalLookupsCount = 0
            sessions.forEach {
                assert(it.completions.size == it.lookups.size)
                totalLookupsCount += it.completions.size
                it.completions.zip(it.lookups).forEach { (completion, lookup) ->
                    val rank = lookup.indexOf(completion) + 1
                    if (rank > 0) {
                        rankSum += 1.0 / rank
                    }
                }
            }
            return rankSum / totalLookupsCount
        }
    }
}