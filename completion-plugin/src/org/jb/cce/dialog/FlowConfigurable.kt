package org.jb.cce.dialog

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import org.jb.cce.util.Config
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class FlowConfigurable : EvaluationConfigurable {
    companion object {
        private const val statsCollectorId = "com.intellij.stats.completion"
    }
    private var interpretActions = false
    private var saveLogs = true
    private var trainTestSplit = 70
    private var workspaceDir = ""
    private val trainTestSpinner = JSpinner(SpinnerNumberModel(trainTestSplit, 1, 99, 1))

    override fun createPanel(previousState: Config): JPanel {
        interpretActions = previousState.interpretActions
        workspaceDir = previousState.workspaceDir
        saveLogs = previousState.saveLogs
        trainTestSplit = previousState.trainTestSplit
        val statsCollectorEnabled = PluginManager.getPlugin(PluginId.getId(statsCollectorId))?.isEnabled ?: false

        return panel(title = "Flow Control", constraints = *arrayOf(LCFlags.noGrid, LCFlags.flowY)) { }.apply {
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("Interpret actions after generation:"))
                add(JCheckBox("", interpretActions).configureInterpretActions())
            })
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("Results workspace directory:"))
                add(JTextField(workspaceDir).configureWorkspace())
            })
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("Save completion logs:"))
                add(JCheckBox("", saveLogs).configureSaveLogs(statsCollectorEnabled))
                add(JLabel("Train/test split percentage:"))
                add(trainTestSpinner.configureTrainTest(statsCollectorEnabled))
            })
        }
    }

    override fun configure(builder: Config.Builder) {
        builder.interpretActions = interpretActions
        builder.workspaceDir = workspaceDir
        builder.saveLogs = saveLogs
        builder.trainTestSplit = trainTestSplit
    }

    private fun JCheckBox.configureInterpretActions(): JCheckBox {
        addItemListener { event ->
            interpretActions = event.stateChange == ItemEvent.SELECTED
        }
        return this
    }

    private fun JTextField.configureWorkspace(): JTextField {
        document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent) = update()
            override fun removeUpdate(e: DocumentEvent) = update()
            override fun insertUpdate(e: DocumentEvent) = update()

            private fun update() {
                workspaceDir = text
            }
        })
        return this
    }

    private fun JSpinner.configureTrainTest(statsCollectorEnabled: Boolean): JSpinner {
        isEnabled = saveLogs && statsCollectorEnabled
        addChangeListener {
            trainTestSplit = value as Int
        }
        return this
    }

    private fun JCheckBox.configureSaveLogs(statsCollectorEnabled: Boolean): JCheckBox {
        if (!statsCollectorEnabled) {
            saveLogs = false
            isSelected = false
            isEnabled = false
        }
        addItemListener { event ->
            saveLogs = event.stateChange == ItemEvent.SELECTED
            trainTestSpinner.isEnabled = saveLogs
        }
        return this
    }
}