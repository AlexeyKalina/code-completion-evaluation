package org.jb.cce.metrics

data class MetricInfo(val name: String, val value: String, val evaluationType: String) {
    val label = "${name.filter { it.isLetterOrDigit() }}$evaluationType"
}