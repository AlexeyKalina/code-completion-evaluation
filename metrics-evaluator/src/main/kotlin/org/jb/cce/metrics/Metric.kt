package org.jb.cce.metrics

import org.jb.cce.Session

interface Metric {
    fun evaluate(sessions: List<Session>): Double
    val name: String
}