package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.util.Sample
import java.util.stream.Collectors

class PrecisionMetric(private val n: Int) : Metric {
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
            if (indexOfNecessaryCompletion in 0 until n) {
                fileSample.add(1.0)
                sample.add(1.0)
            } else if (completion.first.isNotEmpty()) {
                fileSample.add(0.0)
                sample.add(0.0)
            }
        }

        return fileSample.mean()
    }

    override val name: String = "Precision@$n"

    override val valueType = MetricValueType.DOUBLE
}