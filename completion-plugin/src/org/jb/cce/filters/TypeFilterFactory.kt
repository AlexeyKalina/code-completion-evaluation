package org.jb.cce.filters

import org.jb.cce.actions.TypeFilter
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel

class TypeFilterFactory {
    private val types = mutableListOf(TypeFilter.METHOD_CALLS)

    val panel = createTypesPanel()
    fun getFilter() = types

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
        val methodsButton = getTypeCheckBox(TypeFilter.METHOD_CALLS, "Method calls")
        val fieldsButton = getTypeCheckBox(TypeFilter.FIELDS, "Fields")
        val variablesButton = getTypeCheckBox(TypeFilter.VARIABLES, "Variables")

        methodsButton.isSelected = true
        return listOf(methodsButton, fieldsButton, variablesButton)
    }

    private fun getTypeCheckBox(type: TypeFilter, title: String): JCheckBox {
        val typeButton =  JCheckBox(title)
        typeButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                if (!types.contains(type)) types.add(type)
            } else types.remove(type)
        }
        return typeButton
    }
}