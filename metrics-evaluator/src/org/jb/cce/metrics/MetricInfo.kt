package org.jb.cce.metrics

data class MetricInfo(val name: String, val value: String, val evaluationType: String) {
    val label = "$name$evaluationType".filter { it.isLetterOrDigit() }
}