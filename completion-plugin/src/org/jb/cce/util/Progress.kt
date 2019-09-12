package org.jb.cce.util

interface Progress {
    fun setProgress(fileName: String, text: String, fraction: Double)
    fun isCanceled(): Boolean
}