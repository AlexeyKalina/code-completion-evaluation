package org.jb.cce.util

class FakeProgress : Progress {
    override fun isCanceled(): Boolean = false
    override fun setProgress(text: String, fraction: Double) {}
}