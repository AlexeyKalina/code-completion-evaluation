package org.jb.cce.filter.impl

import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.uast.Language
import org.jb.cce.uast.NodeProperties
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class PackagePrefixFilter(private val prefix: String) : EvaluationFilter {
    override fun shouldEvaluate(properties: NodeProperties): Boolean = properties.packageName?.startsWith(prefix) ?: true
}

class PackagePrefixFilterConfiguration: EvaluationFilterConfiguration {
    override val id: String = "packagePrefix"
    override val description: String = "Filter out tokens by package prefix"

    override fun createConfigurable(): EvaluationFilterConfiguration.Configurable = PackagePrefixConfigurable()

    override fun isLanguageSupported(languageName: String): Boolean = Language.JAVA.displayName == languageName

    override fun buildFromJson(json: Any?): EvaluationFilter = if (json == null) EvaluationFilter.ACCEPT_ALL else PackagePrefixFilter(json as String)

    private inner class PackagePrefixConfigurable : EvaluationFilterConfiguration.Configurable {
        private var packagePrefix = ""

        override val panel = createPackagePrefixPanel()

        override fun build(): EvaluationFilter = if (packagePrefix.isEmpty()) EvaluationFilter.ACCEPT_ALL else PackagePrefixFilter(packagePrefix)

        override fun isLanguageSupported(languageName: String): Boolean = this@PackagePrefixFilterConfiguration.isLanguageSupported(languageName)

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
                }
            })

            packagePanel.add(packageLabel)
            packagePanel.add(packageText)

            return packagePanel
        }
    }
}