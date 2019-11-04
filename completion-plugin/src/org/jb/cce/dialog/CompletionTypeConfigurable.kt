package org.jb.cce.dialog

import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import org.jb.cce.actions.CompletionType
import org.jb.cce.util.Config
import java.awt.event.ItemEvent
import javax.swing.JPanel

class CompletionTypeConfigurable : EvaluationConfigurable {
    private var completionType: CompletionType = CompletionType.BASIC

    override fun createPanel(previousState: Config): JPanel {
        completionType = previousState.actionsInterpretation.completionType
        return panel(title = "Completion type", constraints = *arrayOf(LCFlags.noGrid)) {
            buttonGroup {
                row {
                    radioButton("Basic").configure(CompletionType.BASIC)
                    radioButton("Smart").configure(CompletionType.SMART)
                    radioButton("ML").configure(CompletionType.ML)
                }
            }
        }
    }

    override fun configure(builder: Config.Builder) {
        builder.completionType = completionType
        builder.evaluationTitle = completionType.name
    }

    private fun CellBuilder<JBRadioButton>.configure(value: CompletionType) {
        component.isSelected = completionType == value
        component.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) completionType = value
        }
    }
}