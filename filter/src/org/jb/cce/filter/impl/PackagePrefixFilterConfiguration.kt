package org.jb.cce.filter.impl

import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.uast.Language
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class PackagePrefixFilterConfiguration: EvaluationFilterConfiguration {
    override val id: String = "packagePrefix"
    override val description: String = "Filter out tokens by package prefix"

    override fun getConfigurable(): EvaluationFilterConfiguration.Configurable = PackagePrefixConfigurable

    override fun supportedLanguages(): Set<Language> = setOf(Language.JAVA)

    override fun buildFromJson(json: Any): EvaluationFilter {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private object PackagePrefixConfigurable : EvaluationFilterConfiguration.Configurable {
        private var packagePrefix = ""

        override val panel = createPackagePrefixPanel()

        override fun build(): EvaluationFilter = if (packagePrefix.isEmpty()) EvaluationFilter.ACCEPT_ALL else PackagePrefixFilter(packagePrefix)

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