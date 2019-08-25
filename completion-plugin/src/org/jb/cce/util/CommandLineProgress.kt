package org.jb.cce.util

import java.lang.String.join
import java.util.Collections.nCopies
import kotlin.math.log10

class CommandLineProgress(private val title: String) : Progress {
    private val progress = StringBuilder(140)

    override fun setProgress(text: String, fraction: Double) {
        progress.clear()
        val percent = (fraction * 100).toInt()
        progress
                .append('\r')
                .append(join("", nCopies(if (percent == 0) 2 else 2 - log10(percent.toDouble()).toInt(), " ")))
                .append(String.format(" %d%% [", percent))
                .append(join("", nCopies(percent, "=")))
                .append('>')
                .append(join("", nCopies(100 - percent, " ")))
                .append(']')
        println("$title: $text")
        println(progress)
    }

    override fun isCanceled(): Boolean = false
}