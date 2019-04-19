package org.jb.cce.metrics

import org.jb.cce.Session

interface Metric {
    fun evaluate(sessions: List<Session>, update: Boolean = true): Double
    fun clear()
    val aggregatedValue: Double
    val name: String
}