package org.jb.cce.filters

import org.jb.cce.actions.ArgumentFilter
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.*

class ArgumentFilterFactory {
    private var argument = ArgumentFilter.ALL

    val panel = createArgumentPanel()
    fun getFilter() = argument

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