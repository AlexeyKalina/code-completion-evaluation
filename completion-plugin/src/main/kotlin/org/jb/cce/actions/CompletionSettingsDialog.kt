package org.jb.cce.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.Language
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


class CompletionSettingsDialog(project: Project, private val language2files: Map<Language, Set<VirtualFile>>) : DialogWrapper(true) {
    lateinit var language: Language
    private val properties = PropertiesComponent.getInstance(project)
    private val outputDirProperty = "org.jb.cce.output_dir"
    var outputDir = properties.getValue(outputDirProperty) ?: project.basePath ?: ""
    var completionTypes = arrayListOf(CompletionType.BASIC)

    init {
        init()
        title = "Completion evaluation settings"
    }
    var completionContext = CompletionContext.ALL
    var completionPrefix: CompletionPrefix = CompletionPrefix.NoPrefix
    var completionStatement = CompletionStatement.METHOD_CALLS

    override fun createCenterPanel(): JComponent? {
        val dialogPanel = JPanel(GridLayout(6,1))

        dialogPanel.add(createLanguageChooser())
        dialogPanel.add(createTypePanel())
        dialogPanel.add(createContextPanel())
        dialogPanel.add(createPrefixPanel())
        dialogPanel.add(createStatementPanel())
        dialogPanel.add(createOutputDirChooser())

        return dialogPanel
    }

    override fun doValidate(): ValidationInfo? {
        if (completionTypes.isEmpty())
            return ValidationInfo("Select at least one of the completion types.")
        return super.doValidate()
    }

    private class LanguageItem(val language: Language, val count: Int) {
        override fun toString(): String = "${language.displayName} ($count)"
    }

    private fun createLanguageChooser(): JPanel {
        val languageLabel = JLabel("Language:")
        val languagePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        languagePanel.add(languageLabel)

        val languages = language2files.map { LanguageItem(it.key, it.value.size) }
                .sortedByDescending { it.count }.toTypedArray()
        language = languages[0].language
        if (language2files.size == 1) {
            languagePanel.add(JLabel(languages.single().toString()))
        } else {
            val languageComboBox = ComboBox<LanguageItem>(languages)
            languageComboBox.selectedItem = languages.first()
            languageComboBox.addItemListener { event ->
                if (event.stateChange == ItemEvent.SELECTED) {
                    language = (event.item as LanguageItem).language
                }
            }
            languagePanel.add(languageComboBox)
        }
        return languagePanel
    }

    private fun createTypePanel(): JPanel {
        val typeLabel = JLabel("Completion type:")
        val completionTypePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val basicCompletionButton =  JCheckBox("Basic")
        basicCompletionButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED && !completionTypes.contains(CompletionType.BASIC)) completionTypes.add(CompletionType.BASIC)
            else if (event.stateChange == ItemEvent.DESELECTED) completionTypes.removeIf { it == CompletionType.BASIC }
        }
        val smartCompletionButton =  JCheckBox("Smart")
        smartCompletionButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED && !completionTypes.contains(CompletionType.SMART)) completionTypes.add(CompletionType.SMART)
            else if (event.stateChange == ItemEvent.DESELECTED) completionTypes.removeIf { it == CompletionType.SMART }
        }

        basicCompletionButton.isSelected = true

        completionTypePanel.add(typeLabel)
        completionTypePanel.add(basicCompletionButton)
        completionTypePanel.add(smartCompletionButton)

        return completionTypePanel
    }

    private fun createContextPanel(): JPanel {
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

        return contextTypePanel
    }

    private fun createPrefixPanel(): JPanel {
        val prefixLabel = JLabel("Completion prefix:")
        val prefixPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val noPrefixButton =  JRadioButton("No prefix")
        val model = SpinnerNumberModel(2, 1, 5, 1)
        val simplePrefixSpinner = JSpinner(model)
        noPrefixButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                completionPrefix = CompletionPrefix.NoPrefix
                simplePrefixSpinner.isEnabled = false
            }
        }
        val simplePrefixButton =  JRadioButton("Simple prefix")
        simplePrefixSpinner.isEnabled = false
        simplePrefixButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                completionPrefix = CompletionPrefix.SimplePrefix(simplePrefixSpinner.value as Int)
                simplePrefixSpinner.isEnabled = true
            }
        }
        simplePrefixSpinner.addChangeListener {
            completionPrefix = CompletionPrefix.SimplePrefix(simplePrefixSpinner.value as Int)
        }
        val capitalizePrefixButton =  JRadioButton("Capitalize prefix")
        capitalizePrefixButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                completionPrefix = CompletionPrefix.CapitalizePrefix
                simplePrefixSpinner.isEnabled = false
            }
        }

        noPrefixButton.isSelected = true

        val prefixGroup =  ButtonGroup()

        prefixGroup.add(noPrefixButton)
        prefixGroup.add(simplePrefixButton)
        prefixGroup.add(capitalizePrefixButton)

        prefixPanel.add(prefixLabel)
        prefixPanel.add(noPrefixButton)
        prefixPanel.add(simplePrefixButton)
        prefixPanel.add(simplePrefixSpinner)
        prefixPanel.add(capitalizePrefixButton)

        return prefixPanel
    }

    private fun createStatementPanel(): JPanel {
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

        return statementPanel
    }

    private fun createOutputDirChooser(): JPanel {
        val outputLabel = JLabel("Output directory for report:")
        val outputPanel = JPanel(FlowLayout(FlowLayout.LEFT))

        val outDirText = JTextField(outputDir)
        outDirText.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent) = update()
            override fun removeUpdate(e: DocumentEvent) = update()
            override fun insertUpdate(e: DocumentEvent) = update()

            private fun update() {
                outputDir = outDirText.text
                properties.setValue(outputDirProperty, outputDir)
            }
        })

        outputPanel.add(outputLabel)
        outputPanel.add(outDirText)

        return outputPanel
    }
}

