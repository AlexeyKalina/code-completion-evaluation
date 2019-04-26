package org.jb.cce.actions

import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class OpenFolderDialogWrapper : DialogWrapper(true) {
    init {
        init()
        title = "Quality evaluation completed"
    }

    private var isDesktopSupported = true
    private var pathToReports = ""

    fun showAndGet(isDesktopSupported: Boolean, pathToReports: String): Boolean {
        this.isDesktopSupported = isDesktopSupported
        this.pathToReports = pathToReports
        return super.showAndGet()
    }

    override fun createCenterPanel(): JComponent? {
        val dialogPanel = JPanel()
        val message = if (isDesktopSupported) "Do you want to open a folder with reports?"
            else "You can find reports in $pathToReports"
        val typeLabel = JLabel(message)
        dialogPanel.add(typeLabel)
        return dialogPanel
    }

}