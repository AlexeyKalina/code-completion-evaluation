package org.jb.cce.metrics.samples

class AppendableSample : Sample() {
    private var sum = 0.0
    private var count = 0L

    override fun mean() = if (count == 0L) 0.0 else sum / count

    fun add(value: Double, count: Long) {
        this.sum += value
        this.count += count

        val current = value / count
        if (current < min) min = current
        if (current > max) max = current
    }
}