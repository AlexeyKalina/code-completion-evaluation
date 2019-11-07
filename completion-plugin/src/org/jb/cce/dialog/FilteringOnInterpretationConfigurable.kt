package org.jb.cce.dialog

import com.intellij.ui.layout.GrowPolicy
import com.intellij.ui.layout.panel
import org.jb.cce.Config
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class FilteringOnInterpretationConfigurable : EvaluationConfigurable {
    private lateinit var probabilityTextField: JTextField
    private lateinit var seedTextField: JTextField

    override fun createPanel(previousState: Config): JPanel {
        probabilityTextField = JTextField(previousState.interpret.completeTokenProbability.toString())
        seedTextField = JTextField(previousState.interpret.completeTokenSeed?.toString() ?: "").apply {
            isEnabled = 0 < previousState.interpret.completeTokenProbability &&
                    previousState.interpret.completeTokenProbability < 1
        }

        return panel(title = "Filtering tokens during interpretation") {
            row {
                cell {
                    label("Probability of token will be completed:")
                    probabilityTextField(growPolicy = GrowPolicy.SHORT_TEXT).apply {
                        component.document.addDocumentListener(object : DocumentListener {
                            override fun changedUpdate(e: DocumentEvent) = update()
                            override fun removeUpdate(e: DocumentEvent) = update()
                            override fun insertUpdate(e: DocumentEvent) = update()

                            private fun update() {
                                val value = probabilityTextField.text.toDoubleOrNull()
                                seedTextField.isEnabled = value != null && 0 < value && value < 1
                            }
                        })
                    }
                }
            }
            row {
                cell {
                    label("Seed for random:")
                    seedTextField(growPolicy = GrowPolicy.SHORT_TEXT)
                }
            }
        }
    }

    override fun configure(builder: Config.Builder) {
        builder.completeTokenProbability = probabilityTextField.text.toDoubleOrNull() ?: 1.0
        builder.completeTokenSeed = seedTextField.text.toLongOrNull()
    }
}