package org.jb.cce.util

import com.intellij.openapi.progress.ProgressIndicator

class IdeaProgress(private val indicator: ProgressIndicator) : Progress {
    override fun isCanceled(): Boolean = indicator.isCanceled

    override fun setProgress(fileName: String, text: String, fraction: Double) {
        indicator.text2 = text
        indicator.fraction = fraction
    }
}