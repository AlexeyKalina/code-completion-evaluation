package org.jb.cce.filter.impl

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
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

class TypeFilter(val values: List<TypeProperty>) : EvaluationFilter {
    override fun shouldEvaluate(properties: NodeProperties): Boolean = properties.tokenType == null || values.contains(properties.tokenType!!)
    override fun toJson(): JsonElement {
        val json = JsonArray()
        for (value in values)
            json.add(JsonPrimitive(value.name))
        return json
    }
}

class TypeFilterConfiguration : EvaluationFilterConfiguration {
    override val id: String = "statementTypes"
    override val description: String = "Filter out tokens by statement type"

    override fun createConfigurable(previousState: EvaluationFilter): EvaluationFilterConfiguration.Configurable = TypeConfigurable(previousState)

    override fun isLanguageSupported(languageName: String): Boolean = Language.values().any { it.displayName == languageName }

    override fun buildFromJson(json: Any?): EvaluationFilter =
            if (json == null) EvaluationFilter.ACCEPT_ALL
            else TypeFilter((json as List<String>).map { TypeProperty.valueOf(it) })

    override fun defaultFilter(): EvaluationFilter = TypeFilter(listOf(TypeProperty.METHOD_CALL))

    private inner class TypeConfigurable(previousState: EvaluationFilter) : EvaluationFilterConfiguration.Configurable {
        private val types: MutableList<TypeProperty> =
                if (previousState == EvaluationFilter.ACCEPT_ALL) TypeProperty.values().toMutableList()
                else (previousState as TypeFilter).values.toMutableList()

        override val panel: JPanel = createTypesPanel()

        override fun build(): EvaluationFilter {
            return if (types.size == TypeProperty.values().size) EvaluationFilter.ACCEPT_ALL else TypeFilter(types)
        }

        override fun isLanguageSupported(languageName: String): Boolean = this@TypeFilterConfiguration.isLanguageSupported(languageName)

        private fun createTypesPanel(): JPanel {
            val typesPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            typesPanel.add(JLabel("Statements type:"))
            for (type in createTypeCheckBoxes()) {
                typesPanel.add(type)
            }
            return typesPanel
        }

        private fun createTypeCheckBoxes(): List<JCheckBox> = listOf(
                getTypeCheckBox(TypeProperty.METHOD_CALL, "Method calls"),
                getTypeCheckBox(TypeProperty.FIELD, "Fields"),
                getTypeCheckBox(TypeProperty.VARIABLE, "Variables")
        )

        private fun getTypeCheckBox(type: TypeProperty, title: String): JCheckBox =
                JCheckBox(title, types.contains(type)).apply {
                    addItemListener { event ->
                        if (event.stateChange == ItemEvent.SELECTED) {
                            if (!types.contains(type)) types.add(type)
                        } else types.remove(type)
                    }
                }
    }
}