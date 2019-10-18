package org.jb.cce.filter.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.uast.Language
import org.jb.cce.uast.NodeProperties
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class PackageRegexFilter(pattern: String) : EvaluationFilter {
    val regex = Regex(pattern)
    override fun shouldEvaluate(properties: NodeProperties): Boolean = properties.packageName?.matches(regex) ?: true
    override fun toJson(): JsonElement = JsonPrimitive(regex.pattern)
}

class PackageRegexFilterConfiguration: EvaluationFilterConfiguration {
    override val id: String = "packageRegex"
    override val description: String = "Filter out tokens by package name regex"

    override fun createConfigurable(previousState: EvaluationFilter): EvaluationFilterConfiguration.Configurable = PackageRegexConfigurable(previousState)

    override fun isLanguageSupported(languageName: String): Boolean = Language.JAVA.displayName == languageName

    override fun buildFromJson(json: Any?): EvaluationFilter = if (json == null) EvaluationFilter.ACCEPT_ALL else PackageRegexFilter(json as String)

    override fun defaultFilter(): EvaluationFilter = PackageRegexFilter(".*")

    private inner class PackageRegexConfigurable(previousState: EvaluationFilter) : EvaluationFilterConfiguration.Configurable {
        private var packageRegex =
                if (previousState == EvaluationFilter.ACCEPT_ALL) ""
                else (previousState as PackageRegexFilter).regex.pattern

        override val panel = createPackageRegexPanel()

        override fun build(): EvaluationFilter = if (packageRegex.isEmpty()) EvaluationFilter.ACCEPT_ALL else PackageRegexFilter(packageRegex)

        override fun isLanguageSupported(languageName: String): Boolean = this@PackageRegexFilterConfiguration.isLanguageSupported(languageName)

        private fun createPackageRegexPanel(): JPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Package Regex:"))
            add(JTextField(packageRegex).apply {
                document.addDocumentListener(object : DocumentListener {
                    override fun changedUpdate(e: DocumentEvent) = update()
                    override fun removeUpdate(e: DocumentEvent) = update()
                    override fun insertUpdate(e: DocumentEvent) = update()

                    private fun update() {
                        packageRegex = text
                    }
                })
            })
        }
    }
}