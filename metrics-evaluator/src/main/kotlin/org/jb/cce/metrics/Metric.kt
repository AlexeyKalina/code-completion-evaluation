package org.jb.cce.metrics

import org.jb.cce.Session
import org.jb.cce.metrics.samples.Sample

interface Metric {
    fun evaluate(sessions: List<Session>): Double
    val sample : Sample
    val name: String
}