package org.jb.cce.metrics.samples

abstract class Sample {
    protected var min = Double.MAX_VALUE
    protected var max = Double.MIN_VALUE

    abstract fun mean() : Double

    fun min() = if (min == Double.MAX_VALUE) 0.0 else min

    fun max() = if (max == Double.MIN_VALUE) 0.0 else max
}