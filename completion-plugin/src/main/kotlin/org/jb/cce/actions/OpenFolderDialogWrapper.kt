package org.jb.cce.actions

import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class OpenFolderDialogWrapper() : DialogWrapper(true) {
    init {
        init()
        title = "Quality evaluation completed"
    }

    override fun createCenterPanel(): JComponent? {
        val dialogPanel = JPanel()
        val typeLabel = JLabel("Do you want to open a folder with reports?")
        dialogPanel.add(typeLabel)
        return dialogPanel
    }
}