package org.jb.cce.metrics.util

class Sample {
    private var sum = 0.0
    private var count = 0L

    fun mean(): Double = sum / count

    fun add(value: Double) {
        sum += value
        count++
    }
}