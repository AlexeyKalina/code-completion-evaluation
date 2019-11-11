package org.jb.cce.dialog

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import javax.swing.JComponent

class OpenBrowserDialog(private val reportNames: List<String>) : DialogWrapper(true) {
    init {
        init()
        title = "Quality evaluation completed"
    }

    val reportNamesForOpening = mutableSetOf<String>()

    override fun createCenterPanel(): JComponent? = panel {
        row { label("Select reports for opening in the browser:") }
        for (reportName in reportNames) {
            row {
                checkBox(reportName).apply {
                    addItemListener {
                        if (isSelected) reportNamesForOpening.add(text)
                        else reportNamesForOpening.remove(text)
                    }
                }
            }
        }
    }
}