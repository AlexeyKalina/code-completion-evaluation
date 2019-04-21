package org.jb.cce.metrics

import org.jb.cce.Session

class RecallMetric : Metric {
    private var totalCount = 0L
    private var relevantCount = 0L

    override val value: Double
        get() = if (totalCount == 0L) 0.0 else relevantCount.toDouble() / totalCount.toDouble()

    override fun evaluate(sessions: List<Session>): Double {
        val recommendationsMadeCount = sessions.stream()
                .filter { session -> session.lookups.contains(session.completion) }
                .count()

        totalCount += sessions.size
        relevantCount += recommendationsMadeCount
        return recommendationsMadeCount.toDouble() / sessions.size
    }

    override val name: String = "Recall"
}