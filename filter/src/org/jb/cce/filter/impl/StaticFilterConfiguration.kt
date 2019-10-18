package org.jb.cce.filter.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.uast.Language
import org.jb.cce.uast.NodeProperties
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.ButtonGroup
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton

class StaticFilter(val expectedValue: Boolean) : EvaluationFilter {
    override fun shouldEvaluate(properties: NodeProperties): Boolean = properties.isStatic?.equals(expectedValue) ?: true
    override fun toJson(): JsonElement = JsonPrimitive(expectedValue)
}

class StaticFilterConfiguration: EvaluationFilterConfiguration {
    override val id: String = "isStatic"
    override val description: String = "Filter out token if it's static member access"

    override fun createConfigurable(previousState: EvaluationFilter): EvaluationFilterConfiguration.Configurable = StaticConfigurable(previousState)

    override fun isLanguageSupported(languageName: String): Boolean = Language.JAVA.displayName == languageName

    override fun buildFromJson(json: Any?): EvaluationFilter = if (json == null) EvaluationFilter.ACCEPT_ALL else StaticFilter(json as Boolean)

    override fun defaultFilter(): EvaluationFilter = StaticFilter(false)

    private enum class StaticFilter {
        STATIC,
        NOT_STATIC,
        ALL
    }

    private inner class StaticConfigurable(private val previousState: EvaluationFilter) : EvaluationFilterConfiguration.Configurable {
        private var staticType =
                if (previousState is org.jb.cce.filter.impl.StaticFilter) {
                    if (previousState.expectedValue) StaticFilter.STATIC else StaticFilter.NOT_STATIC
                } else StaticFilter.ALL

        override val panel: JPanel = createStaticPanel()

        override fun build(): EvaluationFilter {
            return when (staticType) {
                StaticFilter.ALL -> EvaluationFilter.ACCEPT_ALL
                StaticFilter.NOT_STATIC -> StaticFilter(false)
                StaticFilter.STATIC -> StaticFilter(true)
            }
        }

        override fun isLanguageSupported(languageName: String): Boolean = this@StaticFilterConfiguration.isLanguageSupported(languageName)

        private fun createStaticPanel(): JPanel {
            val staticPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            val staticGroup = ButtonGroup()
            staticPanel.add(JLabel("Static:"))
            for (type in createStaticButtons()) {
                staticPanel.add(type)
                staticGroup.add(type)
            }
            return staticPanel
        }

        private fun createStaticButtons(): List<JRadioButton> = listOf(
                getStaticButton(StaticFilter.STATIC, "Yes"),
                getStaticButton(StaticFilter.NOT_STATIC, "No"),
                getStaticButton(StaticFilter.ALL, "All")
        )

        private fun getStaticButton(value: StaticFilter, title: String): JRadioButton =
                JRadioButton(title, value == staticType).apply {
                    addItemListener { event ->
                        if (event.stateChange == ItemEvent.SELECTED) {
                            staticType = value
                        }
                    }
                }
    }
}