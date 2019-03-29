package org.jb.cce.actions

import com.intellij.openapi.ui.DialogWrapper
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.JPanel

class CompletionSettingsDialogWrapper : DialogWrapper(true) {
    init {
        init()
        title = "Completion evaluation settings"
    }

    var completionType = CompletionType.BASIC
    var completionContext = CompletionContext.ALL
    var completionPrefix: CompletionPrefix = CompletionPrefix.NoPrefix()
    var completionStatement = CompletionStatement.METHOD_CALLS

    override fun createCenterPanel(): JComponent? {
        val dialogPanel = JPanel(GridLayout(4,1))

        val typeLabel = JLabel("Completion type:")
        val completionTypePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val basicCompletionButton =  JRadioButton("Basic")
        basicCompletionButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) completionType = CompletionType.BASIC
        }
        val smartCompletionButton =  JRadioButton("Smart")
        smartCompletionButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) completionType = CompletionType.SMART
        }

        basicCompletionButton.isSelected = true

        val completionTypeGroup =  ButtonGroup()

        completionTypeGroup.add(basicCompletionButton)
        completionTypeGroup.add(smartCompletionButton)

        completionTypePanel.add(typeLabel)
        completionTypePanel.add(basicCompletionButton)
        completionTypePanel.add(smartCompletionButton)

        dialogPanel.add(completionTypePanel)

        val contextLabel = JLabel("Context for completion:")
        val contextTypePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val allContextButton =  JRadioButton("All context")
        allContextButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) completionContext = CompletionContext.ALL
        }
        val previousContextButton =  JRadioButton("Previous context")
        previousContextButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) completionContext = CompletionContext.PREVIOUS
        }

        allContextButton.isSelected = true

        val contextTypeGroup =  ButtonGroup()

        contextTypeGroup.add(allContextButton)
        contextTypeGroup.add(previousContextButton)

        contextTypePanel.add(contextLabel)
        contextTypePanel.add(allContextButton)
        contextTypePanel.add(previousContextButton)

        dialogPanel.add(contextTypePanel)

        val prefixLabel = JLabel("Completion prefix:")
        val prefixPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val noPrefixButton =  JRadioButton("No prefix")
        noPrefixButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) completionPrefix = CompletionPrefix.NoPrefix()
        }
        val simplePrefixButton =  JRadioButton("Simple prefix")
        simplePrefixButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) completionPrefix = CompletionPrefix.SimplePrefix(2)
        }
        val capitalizePrefixButton =  JRadioButton("Capitalize prefix")
        capitalizePrefixButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) completionPrefix = CompletionPrefix.CapitalizePrefix()
        }

        noPrefixButton.isSelected = true

        val prefixGroup =  ButtonGroup()

        prefixGroup.add(noPrefixButton)
        prefixGroup.add(simplePrefixButton)
        prefixGroup.add(capitalizePrefixButton)

        prefixPanel.add(prefixLabel)
        prefixPanel.add(noPrefixButton)
        prefixPanel.add(simplePrefixButton)
        prefixPanel.add(capitalizePrefixButton)

        dialogPanel.add(prefixPanel)

        val statementLabel = JLabel("What complete:")
        val statementPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val methodsButton =  JRadioButton("Method calls")
        methodsButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) completionStatement = CompletionStatement.METHOD_CALLS
        }
        val argumentsButton =  JRadioButton("Method arguments")
        argumentsButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) completionStatement = CompletionStatement.ARGUMENTS
        }
        val variablesButton =  JRadioButton("Variables")
        variablesButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) completionStatement = CompletionStatement.VARIABLES
        }
        val allStatementsButton =  JRadioButton("All of these")
        allStatementsButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) completionStatement = CompletionStatement.ALL
        }

        methodsButton.isSelected = true

        val statementsGroup =  ButtonGroup()

        statementsGroup.add(methodsButton)
        statementsGroup.add(argumentsButton)
        statementsGroup.add(variablesButton)
        statementsGroup.add(allStatementsButton)

        statementPanel.add(statementLabel)
        statementPanel.add(methodsButton)
        statementPanel.add(argumentsButton)
        statementPanel.add(variablesButton)
        statementPanel.add(allStatementsButton)

        dialogPanel.add(statementPanel)

        return dialogPanel
    }
}

