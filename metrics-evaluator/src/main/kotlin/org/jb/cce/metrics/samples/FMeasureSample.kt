package org.jb.cce.metrics.samples

class FMeasureSample(private val precisionSample : Sample,
                     private val recallSample : Sample) : Sample() {

    override fun mean() = 2 * precisionSample.mean() * recallSample.mean() / (precisionSample.mean() + recallSample.mean())

    fun add(value: Double) {
        if (value < min) min = value
        if (value > max) max = value
    }
}