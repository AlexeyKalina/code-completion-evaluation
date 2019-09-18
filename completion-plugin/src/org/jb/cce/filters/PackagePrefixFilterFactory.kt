package org.jb.cce.filters

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import org.jb.cce.actions.CompletionSettingsDialog
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class PackagePrefixFilterFactory(project: Project) {
    companion object {
        const val packagePrefixProperty = "org.jb.cce.filters.package_prefix"
    }
    private val properties = PropertiesComponent.getInstance(project)
    private var packagePrefix = properties.getValue(packagePrefixProperty) ?: ""

    val panel = createPackagePrefixPanel()
    fun getFilter() = packagePrefix

    private fun createPackagePrefixPanel(): JPanel {
        val packageLabel = JLabel("Package Prefix:")
        val packagePanel = JPanel(FlowLayout(FlowLayout.LEFT))

        val packageText = JTextField(packagePrefix)
        packageText.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent) = update()
            override fun removeUpdate(e: DocumentEvent) = update()
            override fun insertUpdate(e: DocumentEvent) = update()

            private fun update() {
                packagePrefix = packageText.text
                properties.setValue(packagePrefixProperty, packagePrefix)
            }
        })

        packagePanel.add(packageLabel)
        packagePanel.add(packageText)

        return packagePanel
    }
}