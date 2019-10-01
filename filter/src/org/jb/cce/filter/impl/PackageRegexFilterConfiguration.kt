package org.jb.cce.filter.impl

import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.uast.Language
import org.jb.cce.uast.NodeProperties
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class PackageRegexFilter(private val regex: String) : EvaluationFilter {
    override fun shouldEvaluate(properties: NodeProperties): Boolean = properties.packageName?.matches(Regex(regex)) ?: true
}

class PackageRegexFilterConfiguration: EvaluationFilterConfiguration {
    override val id: String = "packageRegex"
    override val description: String = "Filter out tokens by package name regex"

    override fun createConfigurable(): EvaluationFilterConfiguration.Configurable = PackageRegexConfigurable()

    override fun isLanguageSupported(languageName: String): Boolean = Language.JAVA.displayName == languageName

    override fun buildFromJson(json: Any?): EvaluationFilter = if (json == null) EvaluationFilter.ACCEPT_ALL else PackageRegexFilter(json as String)

    private inner class PackageRegexConfigurable : EvaluationFilterConfiguration.Configurable {
        private var packageRegex = ""

        override val panel = createPackageRegexPanel()

        override fun build(): EvaluationFilter = if (packageRegex.isEmpty()) EvaluationFilter.ACCEPT_ALL else PackageRegexFilter(packageRegex)

        override fun isLanguageSupported(languageName: String): Boolean = this@PackageRegexFilterConfiguration.isLanguageSupported(languageName)

        private fun createPackageRegexPanel(): JPanel {
            val packageLabel = JLabel("Package Regex:")
            val packagePanel = JPanel(FlowLayout(FlowLayout.LEFT))

            val packageText = JTextField(packageRegex)
            packageText.document.addDocumentListener(object : DocumentListener {
                override fun changedUpdate(e: DocumentEvent) = update()
                override fun removeUpdate(e: DocumentEvent) = update()
                override fun insertUpdate(e: DocumentEvent) = update()

                private fun update() {
                    packageRegex = packageText.text
                }
            })

            packagePanel.add(packageLabel)
            packagePanel.add(packageText)

            return packagePanel
        }
    }
}