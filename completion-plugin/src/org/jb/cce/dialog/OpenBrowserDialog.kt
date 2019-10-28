package org.jb.cce.dialog

import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class OpenBrowserDialog : DialogWrapper(true) {
    init {
        init()
        title = "Quality evaluation completed"
    }

    override fun createCenterPanel(): JComponent? {
        val dialogPanel = JPanel()
        val typeLabel = JLabel("Do you want to open reports in the browser?")
        dialogPanel.add(typeLabel)
        return dialogPanel
    }
}