package org.jb.cce.filter.impl

import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.uast.Language
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel

class TypeFilterConfiguration : EvaluationFilterConfiguration {
    override val id: String = "statementTypes"
    override val description: String = "Filter out tokens by statement type"

    override fun getConfigurable(): EvaluationFilterConfiguration.Configurable = TypeConfigurable

    override fun supportedLanguages(): Set<Language> = Language.values().toSet()

    override fun buildFromJson(json: Any): EvaluationFilter = TypeFilter((json as List<String>).map { TypeFilter.FilterValue.valueOf(it) })

    private object TypeConfigurable : EvaluationFilterConfiguration.Configurable {
        private val types = mutableListOf(TypeFilter.FilterValue.METHOD_CALLS)

        override val panel: JPanel = createTypesPanel()

        override fun build(): EvaluationFilter {
            return if (types.size == TypeFilter.FilterValue.values().size) EvaluationFilter.ACCEPT_ALL else TypeFilter(types)
        }

        private fun createTypesPanel(): JPanel {
            val statementLabel = JLabel("Statements type:")
            val typesPanel = JPanel(FlowLayout(FlowLayout.LEFT))

            typesPanel.add(statementLabel)
            for (type in createTypeCheckBoxes()) {
                typesPanel.add(type)
            }
            return typesPanel
        }

        private fun createTypeCheckBoxes(): List<JCheckBox> {
            val methodsButton = getTypeCheckBox(TypeFilter.FilterValue.METHOD_CALLS, "Method calls")
            val fieldsButton = getTypeCheckBox(TypeFilter.FilterValue.FIELDS, "Fields")
            val variablesButton = getTypeCheckBox(TypeFilter.FilterValue.VARIABLES, "Variables")

            methodsButton.isSelected = true
            return listOf(methodsButton, fieldsButton, variablesButton)
        }

        private fun getTypeCheckBox(type: TypeFilter.FilterValue, title: String): JCheckBox {
            val typeButton =  JCheckBox(title)
            typeButton.addItemListener { event ->
                if (event.stateChange == ItemEvent.SELECTED) {
                    if (!types.contains(type)) types.add(type)
                } else types.remove(type)
            }
            return typeButton
        }
    }
}