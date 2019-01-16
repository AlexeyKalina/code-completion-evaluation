package org.jb.cce.metrics

import org.jb.cce.Session

class ESavedMetricsEvaluator {
    companion object {
        fun evaluate(sessions: List<Session>): Double {
            var eSavedSum = 0.0
            sessions.forEach {
                assert(it.completions.size == it.lookups.size)
                var rank = it.completions.zip(it.lookups).indexOfFirst {
                    (completion, lookup) -> lookup.isNotEmpty() && lookup.first() == completion
                }
                if (rank < 0) {
                    rank = it.completions.size
                }
                eSavedSum += (1.0 - rank.toDouble() / it.completions.size)
            }
            return eSavedSum / sessions.size
        }
    }
}