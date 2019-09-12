package org.jb.cce.util

import java.lang.String.join
import java.util.Collections.nCopies
import kotlin.math.log10

class CommandLineProgress(private val title: String) : Progress {
    private val progress = StringBuilder(140)
    private var currentFile = ""
    private var currentPercent = 0

    override fun setProgress(fileName: String, text: String, fraction: Double) {
        progress.clear()
        val percent = (fraction * 100).toInt()
        progress
                .append('\r')
                .append("$title: $text")
                .append(join("", nCopies(if (percent == 0) 2 else 2 - log10(percent.toDouble()).toInt(), " ")))
                .append(String.format(" %d%% [", percent))
                .append(join("", nCopies(percent, "=")))
                .append('>')
                .append(join("", nCopies(100 - percent, " ")))
                .append(']')
        if (percent != currentPercent || currentFile != fileName) {
            currentPercent = percent
            currentFile = fileName
            println(progress)
        }
    }

    override fun isCanceled(): Boolean = false
}