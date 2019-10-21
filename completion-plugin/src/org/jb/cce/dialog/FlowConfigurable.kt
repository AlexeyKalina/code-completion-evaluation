package org.jb.cce.dialog

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import org.jb.cce.util.Config
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.*

class FlowConfigurable : EvaluationConfigurable {
    companion object {
        private const val statsCollectorId = "com.intellij.stats.completion"
    }
    private var interpretActions = false
    private var saveLogs = true
    private lateinit var workspaceDirTextField: JTextField
    private lateinit var trainTestSpinner: JSpinner

    override fun createPanel(previousState: Config): JPanel {
        interpretActions = previousState.interpretActions
        saveLogs = previousState.saveLogs
        workspaceDirTextField = JTextField(previousState.workspaceDir)
        val statsCollectorEnabled = PluginManager.getPlugin(PluginId.getId(statsCollectorId))?.isEnabled ?: false
        trainTestSpinner = JSpinner(SpinnerNumberModel(previousState.trainTestSplit, 1, 99, 1)).apply {
            isEnabled = saveLogs && statsCollectorEnabled
        }

        return panel(title = "Flow Control", constraints = *arrayOf(LCFlags.noGrid, LCFlags.flowY)) { }.apply {
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("Interpret actions after generation:"))
                add(JCheckBox("", interpretActions).configureInterpretActions())
            })
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("Results workspace directory:"))
                add(workspaceDirTextField)
            })
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JLabel("Save completion logs:"))
                add(JCheckBox("", saveLogs).configureSaveLogs(statsCollectorEnabled))
                add(JLabel("Train/test split percentage:"))
                add(trainTestSpinner)
            })
        }
    }

    override fun configure(builder: Config.Builder) {
        builder.interpretActions = interpretActions
        builder.workspaceDir = workspaceDirTextField.text
        builder.saveLogs = saveLogs
        builder.trainTestSplit = trainTestSpinner.value as Int
    }

    private fun JCheckBox.configureInterpretActions(): JCheckBox {
        addItemListener { event ->
            interpretActions = event.stateChange == ItemEvent.SELECTED
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