package org.jb.cce.dialog

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import javax.swing.JComponent

class OpenBrowserDialog(private val reportNames: List<String>) : DialogWrapper(true) {
    val reportNamesForOpening = mutableSetOf<String>()

    init {
        init()
        title = "Quality evaluation completed"
    }

    override fun createCenterPanel(): JComponent? {
        if (reportNames.size == 1) {
            reportNamesForOpening.add(reportNames.first())
            return panel {
                row { label("Do you want to open report in the browser?") }
            }
        } else {
            return panel {
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
    }
}