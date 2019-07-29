package org.jb.cce.util

interface Progress {
    fun setProgress(text: String, fraction: Double)
    fun isCanceled(): Boolean
}