package org.jb.cce.actions

import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.filter.EvaluationFilterManager
import org.jb.cce.uast.Language
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import java.nio.file.Paths
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.JPanel

class CompletionSettingsDialog(private val project: Project, private val language2files: Map<String, Set<VirtualFile>>,
                               private val fullSettings: Boolean = true) : DialogWrapper(true) {
    constructor(project: Project) : this(project, emptyMap(), false)

    companion object {
        const val completionEvaluationDir = "completion-evaluation"
        const val workspaceDirProperty = "org.jb.cce.workspace_dir"
        private const val previousContextText = "Previous context"
    }
    lateinit var language: String
    private val properties = PropertiesComponent.getInstance(project)

    private val statsCollectorId = "com.intellij.stats.completion"
    var workspaceDir = properties.getValue(workspaceDirProperty) ?: Paths.get(project.basePath ?: "", completionEvaluationDir).toString()
    var completionType = CompletionType.BASIC
    var saveLogs = true
    var trainingPercentage = 70
    var completionContext = CompletionContext.ALL
    var completionPrefix: CompletionPrefix = CompletionPrefix.NoPrefix
    var completeAllTokens = false
    var interpretActionsAfterGeneration = true

    init {
        init()
        title = "Completion evaluation settings"
    }
    private var completeAllTokensPrev = false
    private lateinit var filtersPanel: JPanel
    private lateinit var configurableMap: MutableMap<String, EvaluationFilterConfiguration.Configurable>
    private lateinit var completeAllTokensCheckBox: JCheckBox
    private lateinit var contextButtons: List<JRadioButton>

    fun getFilters(): Map<String, EvaluationFilter> = configurableMap.mapValues { it.value.build() }

    override fun createCenterPanel(): JComponent? {
        val dialogPanel = JPanel()
        dialogPanel.layout = BoxLayout(dialogPanel, BoxLayout.Y_AXIS)

        createContextButtons()
        createCompleteAllTokensCheckBox()
        createFiltersPanel()

        if (fullSettings) dialogPanel.add(createLanguageChooser())
        dialogPanel.add(createTypePanel())
        dialogPanel.add(createContextPanel())
        dialogPanel.add(createPrefixPanel())
        dialogPanel.add(createAllTokensPanel())
        dialogPanel.add(filtersPanel)
        if (fullSettings) dialogPanel.add(createInterpretActionsPanel())
        if (fullSettings) dialogPanel.add(createOutputDirChooser())
        if (fullSettings) dialogPanel.add(createLogsPanel())

        return dialogPanel
    }

    private fun createFiltersPanel() {
        filtersPanel = JPanel()
        filtersPanel.layout = BoxLayout(filtersPanel, BoxLayout.Y_AXIS)
        EvaluationFilterManager.getAllFilters().forEach {
            val configurable = it.createConfigurable()
            configurableMap[it.id] = configurable
            filtersPanel.add(configurable.panel)
        }
    }

    private fun createCompleteAllTokensCheckBox() {
        completeAllTokensCheckBox = JCheckBox()
        completeAllTokensCheckBox.addItemListener {
            completeAllTokens = it.stateChange == ItemEvent.SELECTED
            setPanelEnabled(filtersPanel, it.stateChange != ItemEvent.SELECTED)
            setFiltersByLanguage()
            if (!fullSettings) for (contextButton in contextButtons) {
                if (contextButton.text == previousContextText) contextButton.isEnabled = it.stateChange == ItemEvent.SELECTED
                else if (it.stateChange != ItemEvent.SELECTED) {
                    contextButton.isSelected = true
                    completionContext = CompletionContext.ALL
                }
            }
        }
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
        if (!fullSettings) previousContextButton.isEnabled = false
        contextButtons = listOf(allContextButton, previousContextButton)
    }

    private class LanguageItem(val languageName: String, val count: Int) {
        override fun toString(): String = "$languageName ($count)"
    }

    private fun createLanguageChooser(): JPanel {
        val languageLabel = JLabel("Language:")
        val languagePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        languagePanel.add(languageLabel)

        val languages = language2files.map { LanguageItem(it.key, it.value.size) }
                .sortedByDescending { it.count }.toTypedArray()
        language = languages[0].languageName
        setAllTokens()
        setFiltersByLanguage()
        if (language2files.size == 1) {
            languagePanel.add(JLabel(languages.single().toString()))
        } else {
            val languageComboBox = ComboBox(languages)
            languageComboBox.selectedItem = languages.first()
            languageComboBox.addItemListener { event ->
                if (event.stateChange == ItemEvent.SELECTED) {
                    if (Language.resolve(language) != Language.ANOTHER) completeAllTokensPrev = completeAllTokens
                    language = (event.item as LanguageItem).languageName
                    setAllTokens()
                    setFiltersByLanguage()
                }
            }
            languagePanel.add(languageComboBox)
        }
        return languagePanel
    }

    private fun setFiltersByLanguage() {
        configurableMap.forEach {
            if (!it.value.isLanguageSupported(language)) setPanelEnabled(it.value.panel, false)
            else if (!completeAllTokens) setPanelEnabled(it.value.panel, true)
        }
    }

    private fun setAllTokens() {
        if (Language.resolve(language) == Language.ANOTHER) {
            completeAllTokens = true
            completeAllTokensCheckBox.isSelected = true
            completeAllTokensCheckBox.isEnabled = false
            setPanelEnabled(filtersPanel, false)
        } else {
            completeAllTokensCheckBox.isEnabled = true
            if (!completeAllTokensPrev) {
                completeAllTokens = false
                completeAllTokensCheckBox.isSelected = false
                setPanelEnabled(filtersPanel, true)
            }
        }
    }

    private fun setPanelEnabled(panel: JPanel, isEnabled: Boolean) {
        panel.isEnabled = isEnabled
        for (component in panel.components) {
            if (component is JPanel) {
                setPanelEnabled(component, isEnabled)
            }
            component.isEnabled = isEnabled
        }
    }

    private fun createTypePanel(): JPanel {
        val completionTypePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val typeLabel = JLabel("Completion type:")
        completionTypePanel.add(typeLabel)
        val typeGroup =  ButtonGroup()

        completionTypeButton("Basic", CompletionType.BASIC, typeGroup, completionTypePanel).isSelected = true
        completionTypeButton("Smart", CompletionType.SMART, typeGroup, completionTypePanel)
        completionTypeButton("ML", CompletionType.ML, typeGroup, completionTypePanel)

        return completionTypePanel
    }

    private fun completionTypeButton(name: String, type: CompletionType, group: ButtonGroup, panel: JPanel): JRadioButton {
        val button = JRadioButton(name)
        button.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) completionType = type
        }
        group.add(button)
        panel.add(button)
        return button
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

    private fun createAllTokensPanel(): JPanel {
        val allTokensLabel = JLabel("Complete all tokens:")
        val allTokensPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        allTokensPanel.add(allTokensLabel)
        allTokensPanel.add(completeAllTokensCheckBox)
        return allTokensPanel
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
