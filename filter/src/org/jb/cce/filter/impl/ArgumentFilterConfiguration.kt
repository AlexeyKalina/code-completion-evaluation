package org.jb.cce.filter.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.uast.Language
import org.jb.cce.uast.NodeProperties
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.*

class ArgumentFilter(val expectedValue: Boolean) : EvaluationFilter {
    override fun shouldEvaluate(properties: NodeProperties): Boolean = properties.isArgument?.equals(expectedValue) ?: true
    override fun toJson(): JsonElement = JsonPrimitive(expectedValue)
}

class ArgumentFilterConfiguration: EvaluationFilterConfiguration {
    override val id: String = "isArgument"
    override val description: String = "Filter out token if it's method argument"

    override fun createConfigurable(previousState: EvaluationFilter): EvaluationFilterConfiguration.Configurable = ArgumentConfigurable(previousState)

    override fun isLanguageSupported(languageName: String): Boolean = listOf(Language.JAVA, Language.PYTHON).any { it.displayName == languageName }

    override fun buildFromJson(json: Any?): EvaluationFilter = if (json == null) EvaluationFilter.ACCEPT_ALL else ArgumentFilter(json as Boolean)

    override fun defaultFilter(): EvaluationFilter = EvaluationFilter.ACCEPT_ALL

    private enum class ArgumentFilter {
        ARGUMENT,
        NOT_ARGUMENT,
        ALL
    }

    private inner class ArgumentConfigurable(previousState: EvaluationFilter) : EvaluationFilterConfiguration.Configurable {
        private var argument =
                if (previousState is org.jb.cce.filter.impl.ArgumentFilter) {
                    if (previousState.expectedValue) ArgumentFilter.ARGUMENT else ArgumentFilter.NOT_ARGUMENT
                } else ArgumentFilter.ALL

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
            val argumentPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            val argumentGroup =  ButtonGroup()
            argumentPanel.add(JLabel("Arguments:"))
            for (button in createArgumentButtons()) {
                argumentPanel.add(button)
                argumentGroup.add(button)
            }
            return argumentPanel
        }

        private fun createArgumentButtons(): List<JRadioButton> = listOf(
                getArgumentButton(ArgumentFilter.ARGUMENT, "Yes"),
                getArgumentButton(ArgumentFilter.NOT_ARGUMENT, "No"),
                getArgumentButton(ArgumentFilter.ALL, "All")
        )

        private fun getArgumentButton(value: ArgumentFilter, title: String): JRadioButton =
                JRadioButton(title, value == argument).apply {
                    addItemListener { event ->
                        if (event.stateChange == ItemEvent.SELECTED) {
                            argument = value
                        }
                    }
                }
    }
}