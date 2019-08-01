package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.util.Sample

class MeanReciprocalRankMetric : Metric {
    private val sample = Sample()

    override val value: Double
        get() = sample.mean()

    override fun evaluate(sessions: List<Session>): Double {
        val fileSample = Sample()
        sessions.forEach {
            it.lookups.map { lookup ->  Pair(lookup.suggestions, it.expectedText) }.forEach { (suggests, expectedText) ->
                val rank = suggests.map { suggest -> suggest.text }.indexOf(expectedText) + 1
                val value = if (rank > 0) 1.0 / rank else 0.0
                fileSample.add(value)
                sample.add(value)
            }
        }

        return fileSample.mean()
    }

    override val name: String =  "Mean Reciprocal Rank"
}