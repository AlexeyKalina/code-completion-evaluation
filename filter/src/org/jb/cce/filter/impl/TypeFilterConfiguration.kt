package org.jb.cce.filter.impl

import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.uast.Language
import org.jb.cce.uast.NodeProperties
import org.jb.cce.uast.TypeProperty
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel

class TypeFilter(private val values: List<TypeProperty>) : EvaluationFilter {
    override fun shouldEvaluate(properties: NodeProperties): Boolean = properties.tokenType == null || values.contains(properties.tokenType!!)
}

class TypeFilterConfiguration : EvaluationFilterConfiguration {
    override val id: String = "statementTypes"
    override val description: String = "Filter out tokens by statement type"

    override fun createConfigurable(): EvaluationFilterConfiguration.Configurable = TypeConfigurable()

    override fun isLanguageSupported(languageName: String): Boolean = Language.values().any { it.displayName == languageName }

    override fun buildFromJson(json: Any?): EvaluationFilter =
            if (json == null) EvaluationFilter.ACCEPT_ALL
            else TypeFilter((json as List<String>).map { TypeProperty.valueOf(it) })

    private inner class TypeConfigurable : EvaluationFilterConfiguration.Configurable {
        private val types = mutableListOf(TypeProperty.METHOD_CALL)

        override val panel: JPanel = createTypesPanel()

        override fun build(): EvaluationFilter {
            return if (types.size == TypeProperty.values().size) EvaluationFilter.ACCEPT_ALL else TypeFilter(types)
        }

        override fun isLanguageSupported(languageName: String): Boolean = this@TypeFilterConfiguration.isLanguageSupported(languageName)

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
            val methodsButton = getTypeCheckBox(TypeProperty.METHOD_CALL, "Method calls")
            val fieldsButton = getTypeCheckBox(TypeProperty.FIELD, "Fields")
            val variablesButton = getTypeCheckBox(TypeProperty.VARIABLE, "Variables")

            methodsButton.isSelected = true
            return listOf(methodsButton, fieldsButton, variablesButton)
        }

        private fun getTypeCheckBox(type: TypeProperty, title: String): JCheckBox {
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