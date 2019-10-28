package org.jb.cce.dialog.configurable

import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.Row
import org.jb.cce.filter.EvaluationFilter
import java.awt.event.ItemEvent

class StaticUIConfigurable(previousState: EvaluationFilter, private val layout: LayoutBuilder) : UIConfigurable {
    private enum class StaticFilter {
        STATIC,
        NOT_STATIC,
        ALL
    }

    private var staticType =
            if (previousState is org.jb.cce.filter.impl.StaticFilter) {
                if (previousState.expectedValue) StaticFilter.STATIC else StaticFilter.NOT_STATIC
            } else StaticFilter.ALL

    override val view: Row = createView()

    override fun build(): EvaluationFilter {
        return when (staticType) {
            StaticFilter.ALL -> EvaluationFilter.ACCEPT_ALL
            StaticFilter.NOT_STATIC -> org.jb.cce.filter.impl.StaticFilter(false)
            StaticFilter.STATIC -> org.jb.cce.filter.impl.StaticFilter(true)
        }
    }

    private fun createView(): Row = layout.row {
        buttonGroup {
            cell {
                label("Static:")
                radioButton("Yes").configure(StaticFilter.STATIC)
                radioButton("No").configure(StaticFilter.NOT_STATIC)
                radioButton("All").configure(StaticFilter.ALL)
            }
        }
    }

    private fun CellBuilder<JBRadioButton>.configure(value: StaticFilter) {
        component.isSelected = staticType == value
        component.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) staticType = value
        }
    }
}