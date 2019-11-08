package org.jb.cce.metrics

class MetricInfo(name: String, val value: Double, evaluationType: String, val valueType: MetricValueType) {
    val name = name.filter { it.isLetterOrDigit() }
    val evaluationType = evaluationType.filter { it.isLetterOrDigit() }
}