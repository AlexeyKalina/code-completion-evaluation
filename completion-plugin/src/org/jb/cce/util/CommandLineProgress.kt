package org.jb.cce.util

import java.lang.String.join
import java.util.Collections.nCopies
import kotlin.math.log10

class CommandLineProgress(private val title: String) : Progress {
    companion object {
        private const val progressLengthFactor = 5
    }
    private val progress = StringBuilder(140)
    private var currentFile = ""
    private var currentPercent = 0

    override fun setProgress(fileName: String, text: String, fraction: Double) {
        val percent = (fraction * 100).toInt()
        if (percent == currentPercent && currentFile == fileName) return

        currentPercent = percent
        currentFile = fileName

        progress.clear()
        progress
                .append('\r')
                .append("$title:")
                .append(join("", nCopies(if (percent == 0) 2 else 2 - log10(percent.toDouble()).toInt(), " ")))
                .append(String.format(" %d%% [", percent))
                .append(join("", nCopies(percent / progressLengthFactor, "=")))
                .append('>')
                .append(join("", nCopies((100 - percent) / progressLengthFactor, " ")))
                .append("] $text")
        println(progress)
    }

    override fun isCanceled(): Boolean = false
}