package org.jb.cce.dialog

import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.panel
import org.jb.cce.Config
import javax.swing.JPanel
import javax.swing.JTextField

class FilteringOnInterpretationConfigurable : EvaluationConfigurable {
    private lateinit var probabilityTextField: JTextField

    override fun createPanel(previousState: Config): JPanel {
        probabilityTextField = JTextField(previousState.interpret.completeTokenProbability.toString())

        return panel(title = "Filtering tokens during interpretation") {
            row {
                cell {
                    label("Probability of token will be completed:")
                    probabilityTextField(growPolicy = GrowPolicy.SHORT_TEXT)
                }
            }
        }
    }

    override fun configure(builder: Config.Builder) {
        builder.completeTokenProbability = probabilityTextField.text.toDoubleOrNull() ?: 1.0
    }
}