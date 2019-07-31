package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.util.Sample

open class LatencyMetric {
    protected val sample = Sample()

    protected fun getFileSample(sessions: List<Session>): Sample {
        val fileSample = Sample()
        sessions.stream()
                .flatMap { session -> session.lookups.stream() }
                .forEach {
                    this.sample.add(it.latency.toDouble())
                    fileSample.add(it.latency.toDouble())
                }
        return fileSample
    }
}

class MaxLatencyMetric : LatencyMetric(), Metric {
    override val value: Double
        get() = sample.max()

    override fun evaluate(sessions: List<Session>): Double {
        val fileSample = getFileSample(sessions)
        return fileSample.max()
    }

    override val name: String = "Max Latency"
}

class MeanLatencyMetric : LatencyMetric(), Metric {
    override val value: Double
        get() = sample.mean()

    override fun evaluate(sessions: List<Session>): Double {
        val fileSample = getFileSample(sessions)
        return fileSample.mean()
    }

    override val name: String = "Mean Latency"
}