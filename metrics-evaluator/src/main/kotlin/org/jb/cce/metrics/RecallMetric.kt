package org.jb.cce.metrics

import org.jb.cce.Session

object RecallMetric : Metric {
    private var totalCount = 0L
    private var relevantCount = 0L

    override fun clear() {
        totalCount = 0
        relevantCount = 0
    }

    override val aggregatedValue: Double
        get() = if (totalCount == 0L) 0.0 else relevantCount.toDouble() / totalCount.toDouble()

    override fun evaluate(sessions: List<Session>, update: Boolean): Double {
        val recommendationsMadeCount = sessions.stream()
                .filter { session -> session.lookups.contains(session.completion) }
                .count()

        if (update) {
            totalCount += sessions.size
            relevantCount += recommendationsMadeCount
        }
        return recommendationsMadeCount.toDouble() / sessions.size
    }

    override val name: String = "Recall"
}