package org.jb.cce.metrics

import org.jb.cce.Session

class MeanReciprocalRankMetric : Metric {
    private var totalCount = 0L
    private var rankCount = 0.0

    override val value: Double
        get() = if (totalCount == 0L) 0.0 else rankCount / totalCount.toDouble()

    override fun evaluate(sessions: List<Session>): Double {
        var rankSum = 0.0
        sessions.forEach {
            val rank = it.lookups.indexOf(it.completion) + 1
            if (rank > 0) {
                rankSum += 1.0 / rank
            }
        }

        totalCount += sessions.size
        rankCount += rankSum
        return rankSum / sessions.size
    }

    override val name: String =  "Mean Reciprocal Rank"
}