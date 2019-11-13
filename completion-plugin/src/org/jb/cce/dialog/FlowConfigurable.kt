package org.jb.cce.dialog

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.ui.layout.panel
import org.jb.cce.Config
import java.awt.event.ItemEvent
import javax.swing.*

class FlowConfigurable : EvaluationConfigurable {
    companion object {
        private const val statsCollectorId = "com.intellij.stats.completion"
    }
    private var saveLogs = true
    private lateinit var workspaceDirTextField: JTextField
    private lateinit var trainTestSpinner: JSpinner

    override fun createPanel(previousState: Config): JPanel {
        saveLogs = previousState.interpret.saveLogs
        workspaceDirTextField = JTextField(previousState.outputDir)
        val statsCollectorEnabled = PluginManager.getPlugin(PluginId.getId(statsCollectorId))?.isEnabled ?: false
        trainTestSpinner = JSpinner(SpinnerNumberModel(previousState.interpret.trainTestSplit, 1, 99, 1)).apply {
            isEnabled = saveLogs && statsCollectorEnabled
        }

        return panel(title = "Flow Control") {
            row {
                cell {
                    label("Results workspace directory:")
                    workspaceDirTextField()
                }
            }
            row {
                cell {
                    label("Save completion logs:")
                    checkBox("", saveLogs).configureSaveLogs(statsCollectorEnabled)
                }
            }
            row {
                cell {
                    label("Train/test split percentage:")
                    trainTestSpinner()
                }
            }
        }
    }

    override fun configure(builder: Config.Builder) {
        builder.outputDir = workspaceDirTextField.text
        builder.saveLogs = saveLogs
        builder.trainTestSplit = trainTestSpinner.value as Int
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