package org.jb.cce.actions

import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.uast.Language
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.ItemEvent
import java.nio.file.Paths
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class CompletionSettingsDialog(project: Project, private val language2files: Map<String, Set<VirtualFile>>,
                               private val fullSettings: Boolean = true) : DialogWrapper(true) {
    constructor(project: Project) : this(project, emptyMap(), false)

    companion object {
        const val completionEvaluationDir = "completion-evaluation"
        const val workspaceDirProperty = "org.jb.cce.workspace_dir"
        private const val allTokensText = "All tokens"
        private const val previousContextText = "Previous context"
    }
    lateinit var language: String
    private val properties = PropertiesComponent.getInstance(project)

    private val statsCollectorId = "com.intellij.stats.completion"
    var workspaceDir = properties.getValue(workspaceDirProperty) ?: Paths.get(project.basePath ?: "", completionEvaluationDir).toString()
    var completionTypes = arrayListOf(CompletionType.BASIC)
    var saveLogs = true
    var trainingPercentage = 70
    var completionContext = CompletionContext.ALL
    var completionPrefix: CompletionPrefix = CompletionPrefix.NoPrefix
    var completionStatement = CompletionStatement.METHOD_CALLS

    var interpretActionsAfterGeneration = true

    init {
        init()
        title = "Completion evaluation settings"
    }
    lateinit var statementButtons: List<JRadioButton>
    lateinit var contextButtons: List<JRadioButton>

    override fun createCenterPanel(): JComponent? {
        val dialogPanel = JPanel(GridLayout(if (fullSettings) 8 else 4,1))

        createContextButtons()
        createStatementButtons(!fullSettings)

        if (fullSettings) dialogPanel.add(createLanguageChooser())
        dialogPanel.add(createTypePanel())
        dialogPanel.add(createContextPanel())
        dialogPanel.add(createPrefixPanel())
        dialogPanel.add(createStatementPanel())
        if (fullSettings) dialogPanel.add(createInterpretActionsPanel())
        if (fullSettings) dialogPanel.add(createOutputDirChooser())
        if (fullSettings) dialogPanel.add(createLogsPanel())

        return dialogPanel
    }

    override fun doValidate(): ValidationInfo? {
        if (completionTypes.isEmpty())
            return ValidationInfo("Select at least one of the completion types.")
        return super.doValidate()
    }

    private fun createStatementButtons(blockPreviousContext: Boolean) {
        val methodsButton = getStatementButton(CompletionStatement.METHOD_CALLS, "Method calls", blockPreviousContext)
        val argumentsButton = getStatementButton(CompletionStatement.ARGUMENTS, "Method arguments", blockPreviousContext)
        val variablesButton = getStatementButton(CompletionStatement.VARIABLES, "Variables", blockPreviousContext)
        val staticStatementsButton = getStatementButton(CompletionStatement.ALL_STATIC, "All static members", blockPreviousContext)
        val allStatementsButton = getStatementButton(CompletionStatement.ALL, "All members", blockPreviousContext)
        val allTokensButton = getStatementButton(CompletionStatement.ALL_TOKENS, allTokensText, false)

        methodsButton.isSelected = true
        statementButtons = listOf(methodsButton, argumentsButton, variablesButton, staticStatementsButton, allStatementsButton, allTokensButton)
    }

    private fun getStatementButton(statement: CompletionStatement, title: String, blockPreviousContext: Boolean): JRadioButton {
        val statementButton =  JRadioButton(title)
        statementButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                completionStatement = statement
                for (context in contextButtons)
                    if (context.text == previousContextText && blockPreviousContext) {
                        context.isSelected = false
                        context.isEnabled = false
                    } else if (blockPreviousContext) {
                        context.isSelected = true
                        completionContext = CompletionContext.ALL
                    } else context.isEnabled = true
            }
        }
        return statementButton
    }

    private fun createContextButtons() {
        val allContextButton =  JRadioButton("All context")
        allContextButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) completionContext = CompletionContext.ALL
        }
        val previousContextButton =  JRadioButton(previousContextText)
        previousContextButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) completionContext = CompletionContext.PREVIOUS
        }

        allContextButton.isSelected = true
        contextButtons = listOf(allContextButton, previousContextButton)
    }

    private class LanguageItem(val languageName: String, val count: Int) {
        override fun toString(): String = "${languageName} ($count)"
    }

    private fun createLanguageChooser(): JPanel {
        val languageLabel = JLabel("Language:")
        val languagePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        languagePanel.add(languageLabel)

        val languages = language2files.map { LanguageItem(it.key, it.value.size) }
                .sortedByDescending { it.count }.toTypedArray()
        language = languages[0].languageName
        setStatements()
        if (language2files.size == 1) {
            languagePanel.add(JLabel(languages.single().toString()))
        } else {
            val languageComboBox = ComboBox(languages)
            languageComboBox.selectedItem = languages.first()
            languageComboBox.addItemListener { event ->
                if (event.stateChange == ItemEvent.SELECTED) {
                    language = (event.item as LanguageItem).languageName
                    setStatements()
                }
            }
            languagePanel.add(languageComboBox)
        }
        return languagePanel
    }

    private fun setStatements() {
        if (Language.resolve(language) == Language.ANOTHER) {
            completionStatement = CompletionStatement.ALL_TOKENS
            for (statement in statementButtons)
                if (statement.text != allTokensText) {
                    statement.isSelected = false
                    statement.isEnabled = false
                }
                else statement.isSelected = true
        } else {
            for (statement in statementButtons)
                if (statement.text != allTokensText) statement.isEnabled = true
        }
    }

    private fun createTypePanel(): JPanel {
        val typeLabel = JLabel("Completion type:")
        val completionTypePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val basicCompletionCheckbox = completionTypeCheckbox("Basic", CompletionType.BASIC)
        val smartCompletionCheckbox = completionTypeCheckbox("Smart", CompletionType.SMART)
        val mlCompletionCheckbox = completionTypeCheckbox("ML", CompletionType.ML)

        basicCompletionCheckbox.isSelected = true

        completionTypePanel.add(typeLabel)
        completionTypePanel.add(basicCompletionCheckbox)
        completionTypePanel.add(smartCompletionCheckbox)
        completionTypePanel.add(mlCompletionCheckbox)

        return completionTypePanel
    }

    private fun completionTypeCheckbox(name: String, type: CompletionType): JCheckBox {
        val checkBox = JCheckBox(name)
        checkBox.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED && !completionTypes.contains(type)) completionTypes.add(type)
            else if (event.stateChange == ItemEvent.DESELECTED) completionTypes.removeIf { it == type }
        }
        return checkBox
    }

    private fun createContextPanel(): JPanel {
        val contextLabel = JLabel("Context for completion:")
        val contextTypePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        contextTypePanel.add(contextLabel)
        val contextTypeGroup =  ButtonGroup()
        for (context in contextButtons) {
            contextTypeGroup.add(context)
            contextTypePanel.add(context)
        }
        return contextTypePanel
    }

    private fun createPrefixPanel(): JPanel {
        val prefixLabel = JLabel("Completion prefix:")
        val emulateTypingLabel = JLabel("Emulate typing")

        val prefixPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val noPrefixButton =  JRadioButton("No prefix")
        val model = SpinnerNumberModel(2, 1, 5, 1)
        val simplePrefixSpinner = JSpinner(model)

        val emulateTypingCheckbox = JCheckBox("", completionPrefix.emulateTyping)
        emulateTypingCheckbox.isEnabled = false
        emulateTypingCheckbox.addItemListener { event ->
            completionPrefix = when(completionPrefix) {
                is CompletionPrefix.SimplePrefix -> CompletionPrefix.SimplePrefix(event.stateChange == ItemEvent.SELECTED, simplePrefixSpinner.value as Int)
                is CompletionPrefix.CapitalizePrefix -> CompletionPrefix.CapitalizePrefix(event.stateChange == ItemEvent.SELECTED)
                is CompletionPrefix.NoPrefix -> CompletionPrefix.NoPrefix
            }
        }

        noPrefixButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                completionPrefix = CompletionPrefix.NoPrefix
                simplePrefixSpinner.isEnabled = false
                emulateTypingCheckbox.isSelected = false
                emulateTypingCheckbox.isEnabled = false
            }
        }
        val simplePrefixButton =  JRadioButton("Simple prefix")
        simplePrefixSpinner.isEnabled = false
        simplePrefixButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                completionPrefix = CompletionPrefix.SimplePrefix(emulateTypingCheckbox.isSelected, simplePrefixSpinner.value as Int)
                simplePrefixSpinner.isEnabled = true
                emulateTypingCheckbox.isEnabled = true
            }
        }
        simplePrefixSpinner.addChangeListener {
            completionPrefix = CompletionPrefix.SimplePrefix(emulateTypingCheckbox.isSelected, simplePrefixSpinner.value as Int)
        }
        val capitalizePrefixButton =  JRadioButton("Capitalize prefix")
        capitalizePrefixButton.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                completionPrefix = CompletionPrefix.CapitalizePrefix(emulateTypingCheckbox.isSelected)
                simplePrefixSpinner.isEnabled = false
                emulateTypingCheckbox.isEnabled = true
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
        prefixPanel.add(emulateTypingCheckbox)
        prefixPanel.add(emulateTypingLabel)

        return prefixPanel
    }

    private fun createStatementPanel(): JPanel {
        val statementLabel = JLabel("What complete:")
        val statementPanel = JPanel(FlowLayout(FlowLayout.LEFT))

        val statementsGroup =  ButtonGroup()
        statementPanel.add(statementLabel)
        for (statement in statementButtons) {
            statementsGroup.add(statement)
            statementPanel.add(statement)
        }
        return statementPanel
    }

    private fun createOutputDirChooser(): JPanel {
        val outputLabel = JLabel("Results workspace directory:")
        val outputPanel = JPanel(FlowLayout(FlowLayout.LEFT))

        val outDirText = JTextField(workspaceDir)
        outDirText.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent) = update()
            override fun removeUpdate(e: DocumentEvent) = update()
            override fun insertUpdate(e: DocumentEvent) = update()

            private fun update() {
                workspaceDir = outDirText.text
                properties.setValue(workspaceDirProperty, workspaceDir)
            }
        })

        outputPanel.add(outputLabel)
        outputPanel.add(outDirText)

        return outputPanel
    }

    private fun createLogsPanel(): JPanel {
        val statsCollectorEnabled = PluginManager.getPlugin(PluginId.getId(statsCollectorId))?.isEnabled ?: false

        val logsPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val logsActionLabel = JLabel("Save completion logs:")
        val saveLogsCheckbox = JCheckBox("", saveLogs)
        val model = SpinnerNumberModel(trainingPercentage, 1, 99, 1)
        val trainingPercentageSpinner = JSpinner(model)

        saveLogsCheckbox.addItemListener { event ->
            saveLogs = event.stateChange == ItemEvent.SELECTED
            trainingPercentageSpinner.isEnabled = saveLogs
        }

        if (!statsCollectorEnabled) {
            saveLogs = false
            saveLogsCheckbox.isSelected = false
            saveLogsCheckbox.isEnabled = false
            trainingPercentageSpinner.isEnabled = false
        }

        val trainingPercentageLabel = JLabel("Percent for training:")
        trainingPercentageSpinner.addChangeListener {
            trainingPercentage = trainingPercentageSpinner.value as Int
        }

        logsPanel.add(logsActionLabel)
        logsPanel.add(saveLogsCheckbox)
        logsPanel.add(trainingPercentageLabel)
        logsPanel.add(trainingPercentageSpinner)

        return logsPanel
    }

    private fun createInterpretActionsPanel(): JPanel {
        val interpretActionsLabel = JLabel("Interpret actions after generation:")
        val interpretActionsPanel = JPanel(FlowLayout(FlowLayout.LEFT))

        val interpretActionsCheckbox = JCheckBox("", interpretActionsAfterGeneration)
        interpretActionsCheckbox.addItemListener { event ->
            interpretActionsAfterGeneration = event.stateChange == ItemEvent.SELECTED
        }

        interpretActionsPanel.add(interpretActionsLabel)
        interpretActionsPanel.add(interpretActionsCheckbox)

        return interpretActionsPanel
    }
}
