package org.jb.cce.metrics.util

class Sample {
    private var sum = 0.0
    private var count = 0L
    private var max = Double.NaN

    fun mean(): Double = sum / count
    fun max(): Double = max

    fun add(value: Double) {
        if (max.isNaN() || max < value) max = value
        sum += value
        count++
    }
}