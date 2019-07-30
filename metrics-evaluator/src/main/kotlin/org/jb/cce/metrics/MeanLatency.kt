package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.util.Sample

class MeanLatency : Metric {
    private val sample = Sample()

    override val value: Double
        get() = sample.mean()

    override fun evaluate(sessions: List<Session>): Double {
        val listOfCompletions = sessions.stream()
                .flatMap { session -> session.lookups.stream() }
        var totalLatency = 0L
        var completionsCount = 0
        for (completion in listOfCompletions) {
            totalLatency += completion.latency
            sample.add(completion.latency.toDouble())
            completionsCount++
        }

        return totalLatency.toDouble() / completionsCount
    }

    override val name: String = "Mean Latency"
}