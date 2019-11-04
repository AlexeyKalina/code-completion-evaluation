package org.jb.cce.dialog

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.ui.layout.panel
import org.jb.cce.util.Config
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
        interpretActions = previousState.actionsGeneration.interpretActions
        saveLogs = previousState.actionsInterpretation.saveLogs
        workspaceDirTextField = JTextField(previousState.actionsGeneration.outputDir)
        val statsCollectorEnabled = PluginManager.getPlugin(PluginId.getId(statsCollectorId))?.isEnabled ?: false
        trainTestSpinner = JSpinner(SpinnerNumberModel(previousState.actionsInterpretation.trainTestSplit, 1, 99, 1)).apply {
            isEnabled = saveLogs && statsCollectorEnabled
        }

        return panel(title = "Flow Control") {
            row {
                cell {
                    label("Interpret actions after generation:")
                    checkBox("", interpretActions).configureInterpretActions()
                }
            }
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
        builder.interpretActions = interpretActions
        builder.outputDir = workspaceDirTextField.text
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