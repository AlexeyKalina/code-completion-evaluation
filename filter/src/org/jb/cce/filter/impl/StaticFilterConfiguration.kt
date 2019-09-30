package org.jb.cce.filter.impl

import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.uast.Language
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.ButtonGroup
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton

class StaticFilterConfiguration: EvaluationFilterConfiguration {
    override val id: String = "isStatic"
    override val description: String = "Filter out token if it's static member access"

    override fun getConfigurable(): EvaluationFilterConfiguration.Configurable = StaticConfigurable

    override fun supportedLanguages(): Set<Language> = setOf(Language.JAVA)

    override fun buildFromJson(json: Any): EvaluationFilter {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private object StaticConfigurable : EvaluationFilterConfiguration.Configurable {
        private enum class StaticFilter {
            STATIC,
            NOT_STATIC,
            ALL
        }

        private var staticType = StaticFilter.ALL

        override val panel: JPanel = createStaticPanel()

        override fun build(): EvaluationFilter {
            return when (staticType) {
                StaticFilter.ALL -> EvaluationFilter.ACCEPT_ALL
                StaticFilter.NOT_STATIC -> StaticFilter(false)
                StaticFilter.STATIC -> StaticFilter(true)
            }
        }

        private fun createStaticPanel(): JPanel {
            val staticLabel = JLabel("Static:")
            val staticPanel = JPanel(FlowLayout(FlowLayout.LEFT))

            val staticGroup = ButtonGroup()
            staticPanel.add(staticLabel)
            for (type in createStaticButtons()) {
                staticPanel.add(type)
                staticGroup.add(type)
            }
            return staticPanel
        }

        private fun createStaticButtons(): List<JRadioButton> {
            val staticButton = getStaticButton(StaticFilter.STATIC, "Yes")
            val notStaticButton = getStaticButton(StaticFilter.NOT_STATIC, "No")
            val allButton = getStaticButton(StaticFilter.ALL, "All")

            allButton.isSelected = true
            return listOf(staticButton, notStaticButton, allButton)
        }

        private fun getStaticButton(value: StaticFilter, title: String): JRadioButton {
            val typeButton = JRadioButton(title)
            typeButton.addItemListener { event ->
                if (event.stateChange == ItemEvent.SELECTED) {
                    staticType = value
                }
            }
            return typeButton
        }
    }
}