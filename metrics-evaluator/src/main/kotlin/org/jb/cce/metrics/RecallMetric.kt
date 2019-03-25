package org.jb.cce.metrics

import org.jb.cce.Session
import java.util.stream.Collectors

object RecallMetric : Metric {
    override fun evaluate(sessions: List<Session>): Double {
        val listOfCompletions = sessions.stream()
                .flatMap { compl -> compl.completions.zip(compl.lookups).stream() }
                .collect(Collectors.toList())
        val recommendationsMadeCount = listOfCompletions.stream()
                .filter { (completion, lookup) -> lookup.contains(completion) }
                .count()
        return recommendationsMadeCount.toDouble() / listOfCompletions.size
    }

    override val name: String = "Recall"
}