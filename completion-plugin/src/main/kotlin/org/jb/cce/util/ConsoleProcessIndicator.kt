package org.jb.cce.util

import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import java.lang.String.join
import java.util.Collections.nCopies
import kotlin.math.log10


class ConsoleProcessIndicator(task: Task.Backgroundable, private val isHeadless: Boolean) : BackgroundableProcessIndicator(task) {
    private val progress = StringBuilder(140)

    override fun setFraction(fraction: Double) {
        super.setFraction(fraction)
        if (isHeadless) {
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
            println("$title: $text2")
            println(progress)
        }
    }
}