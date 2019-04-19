package org.jb.cce.metrics

import org.jb.cce.Session

object MeanReciprocalRankMetric : Metric {
    private var totalCount = 0L
    private var rankCount = 0.0

    override fun clear() {
        totalCount = 0
        rankCount = 0.0
    }

    override val aggregatedValue: Double
        get() = if (totalCount == 0L) 0.0 else rankCount / totalCount.toDouble()

    override fun evaluate(sessions: List<Session>, update: Boolean): Double {
        var rankSum = 0.0
        sessions.forEach {
            val rank = it.lookups.indexOf(it.completion) + 1
            if (rank > 0) {
                rankSum += 1.0 / rank
            }
        }
        if (update) {
            totalCount += sessions.size
            rankCount += rankSum
        }
        return rankSum / sessions.size
    }

    override val name: String =  "Mean Reciprocal Rank"
}