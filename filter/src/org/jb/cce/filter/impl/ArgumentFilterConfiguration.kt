package org.jb.cce.filter.impl

import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.uast.Language
import org.jb.cce.uast.NodeProperties
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.*

class ArgumentFilter(private val expectedValue: Boolean) : EvaluationFilter {
    override fun shouldEvaluate(properties: NodeProperties): Boolean = properties.isArgument?.equals(expectedValue) ?: true
}

class ArgumentFilterConfiguration: EvaluationFilterConfiguration {
    override val id: String = "isArgument"
    override val description: String = "Filter out token if it's method argument"

    override fun createConfigurable(): EvaluationFilterConfiguration.Configurable = ArgumentConfigurable()

    override fun isLanguageSupported(languageName: String): Boolean = listOf(Language.JAVA, Language.PYTHON).any { it.displayName == languageName }

    override fun buildFromJson(json: Any): EvaluationFilter = ArgumentFilter(json as Boolean)

    private enum class ArgumentFilter {
        ARGUMENT,
        NOT_ARGUMENT,
        ALL
    }

    private inner class ArgumentConfigurable : EvaluationFilterConfiguration.Configurable {
        private var argument = ArgumentFilter.ALL

        override val panel: JPanel = createArgumentPanel()

        override fun build(): EvaluationFilter {
            return when (argument) {
                ArgumentFilter.ALL -> EvaluationFilter.ACCEPT_ALL
                ArgumentFilter.NOT_ARGUMENT -> ArgumentFilter(false)
                ArgumentFilter.ARGUMENT -> ArgumentFilter(true)
            }
        }

        override fun isLanguageSupported(languageName: String): Boolean = this@ArgumentFilterConfiguration.isLanguageSupported(languageName)

        private fun createArgumentPanel(): JPanel {
            val argumentLabel = JLabel("Arguments:")
            val argumentPanel = JPanel(FlowLayout(FlowLayout.LEFT))

            val argumentGroup =  ButtonGroup()
            argumentPanel.add(argumentLabel)
            for (type in createArgumentButtons()) {
                argumentPanel.add(type)
                argumentGroup.add(type)
            }
            return argumentPanel
        }

        private fun createArgumentButtons(): List<JRadioButton> {
            val argumentButton = getArgumentButton(ArgumentFilter.ARGUMENT, "Yes")
            val notArgumentButton = getArgumentButton(ArgumentFilter.NOT_ARGUMENT, "No")
            val allButton = getArgumentButton(ArgumentFilter.ALL, "All")

            allButton.isSelected = true
            return listOf(argumentButton, notArgumentButton, allButton)
        }

        private fun getArgumentButton(value: ArgumentFilter, title: String): JRadioButton {
            val typeButton =  JRadioButton(title)
            typeButton.addItemListener { event ->
                if (event.stateChange == ItemEvent.SELECTED) {
                    argument = value
                }
            }
            return typeButton
        }
    }
}