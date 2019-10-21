package org.jb.cce.metrics

data class MetricInfo(val name: String, val value: Double, val evaluationType: String, val valueType: MetricValueType) {
    val label = "$name$evaluationType".filter { it.isLetterOrDigit() }
}