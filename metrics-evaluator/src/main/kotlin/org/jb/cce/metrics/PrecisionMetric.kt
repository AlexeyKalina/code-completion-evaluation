package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.util.Sample
import java.util.stream.Collectors

abstract class PrecisionMetric : Metric {
    private val sample = Sample()

    override val value: Double
        get() = sample.mean()

    override fun evaluate(sessions: List<Session>): Double {
        val listOfCompletions = sessions.stream()
                .flatMap { session -> session.lookups.map { lookup -> Pair(lookup.suggestions, session.expectedText) }.stream() }
                .collect(Collectors.toList())

        val fileSample = Sample()
        for (completion in listOfCompletions) {
            val indexOfNecessaryCompletion = completion.first.map { it.text }.indexOf(completion.second)
            if (indexOfNecessaryCompletion in 0 until level()) {
                fileSample.add(1.0)
                sample.add(1.0)
            } else if (completion.first.isNotEmpty()) {
                fileSample.add(0.0)
                sample.add(0.0)
            }
        }

        return fileSample.mean()
    }

    abstract fun level(): Int
}

class PrecisionOneMetric : PrecisionMetric() {
    override fun level() = 1
    override val name: String = "Precision@1"
}

class PrecisionFiveMetric : PrecisionMetric() {
    override fun level() = 5
    override val name: String = "Precision@5"
}