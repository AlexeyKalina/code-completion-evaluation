package org.jb.cce.dialog.configurable

import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.Row
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.impl.ArgumentFilter
import java.awt.event.ItemEvent

class ArgumentUIConfigurable(previousState: EvaluationFilter, private val layout: LayoutBuilder) : UIConfigurable {
    private enum class ArgumentFilter {
        ARGUMENT,
        NOT_ARGUMENT,
        ALL
    }

    private var argument =
            if (previousState is org.jb.cce.filter.impl.ArgumentFilter) {
                if (previousState.expectedValue) ArgumentFilter.ARGUMENT else ArgumentFilter.NOT_ARGUMENT
            } else ArgumentFilter.ALL

    override val view: Row = createView()

    override fun build(): EvaluationFilter {
        return when (argument) {
            ArgumentFilter.ALL -> EvaluationFilter.ACCEPT_ALL
            ArgumentFilter.NOT_ARGUMENT -> ArgumentFilter(false)
            ArgumentFilter.ARGUMENT -> ArgumentFilter(true)
        }
    }

    private fun createView(): Row = layout.row  {
        buttonGroup {
            cell {
                label("Arguments:")
                radioButton("Yes").configure(ArgumentFilter.ARGUMENT)
                radioButton("No").configure(ArgumentFilter.NOT_ARGUMENT)
                radioButton("All").configure(ArgumentFilter.ALL)
            }
        }
    }

    private fun CellBuilder<JBRadioButton>.configure(value: ArgumentFilter) {
        component.isSelected = argument == value
        component.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) argument = value
        }
    }
}