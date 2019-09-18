package org.jb.cce.filters

import org.jb.cce.actions.StaticFilter
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.*

class StaticFilterFactory {
    private var staticType = StaticFilter.ALL

    val panel = createStaticPanel()
    fun getFilter() = staticType

    private fun createStaticPanel(): JPanel {
        val staticLabel = JLabel("Static:")
        val staticPanel = JPanel(FlowLayout(FlowLayout.LEFT))

        val staticGroup =  ButtonGroup()
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
        val typeButton =  JRadioButton(title)
        typeButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                staticType = value
            }
        }
        return typeButton
    }
}