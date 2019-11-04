package org.jb.cce.dialog

import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import com.intellij.util.EventDispatcher
import org.jb.cce.actions.CompletionContext
import org.jb.cce.util.Config
import java.awt.event.ItemEvent
import javax.swing.JPanel

class ContextConfigurable(private val dispatcher: EventDispatcher<SettingsListener>? = null) : EvaluationConfigurable {
    private var context: CompletionContext = CompletionContext.ALL

    override fun createPanel(previousState: Config): JPanel {
        context = previousState.actionsGeneration.strategy.context
        return panel(title = "Context for completion:", constraints = *arrayOf(LCFlags.noGrid)) {
            buttonGroup {
                row {
                    radioButton("All context").configure(CompletionContext.ALL)
                    radioButton("Previous context").configure(CompletionContext.PREVIOUS)
                }
            }
        }
    }

    override fun configure(builder: Config.Builder) {
        builder.contextStrategy = context
    }

    private fun CellBuilder<JBRadioButton>.configure(value: CompletionContext) {
        component.isSelected = context == value
        component.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) context = value
        }
        dispatcher?.addListener(object : SettingsListener {
            override fun allTokensChanged(selected: Boolean) {
                if (value == CompletionContext.PREVIOUS) component.isEnabled = selected
                else if (!selected) {
                    component.isSelected = true
                    context = CompletionContext.ALL
                }
            }
        })
    }
}
