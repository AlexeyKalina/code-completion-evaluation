package org.jb.cce.dialog.configurable

import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.Row
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.impl.TypeFilter
import org.jb.cce.uast.TypeProperty
import java.awt.event.ItemEvent
import javax.swing.JCheckBox

class TypeUIConfigurable(previousState: EvaluationFilter, private val layout: LayoutBuilder) : UIConfigurable {
    private val types: MutableList<TypeProperty> =
            if (previousState == EvaluationFilter.ACCEPT_ALL) TypeProperty.values().toMutableList()
            else (previousState as TypeFilter).values.toMutableList()

    override val view: Row = createView()

    override fun build(): EvaluationFilter {
        return if (types.size == TypeProperty.values().size) EvaluationFilter.ACCEPT_ALL else TypeFilter(types)
    }

    private fun createView(): Row = layout.row {
        cell {
            label("Statements type:")
            checkBox("Method calls").configure(TypeProperty.METHOD_CALL)
            checkBox("Fields").configure(TypeProperty.FIELD)
            checkBox("Variables").configure(TypeProperty.VARIABLE)
        }
    }

    private fun JCheckBox.configure(value: TypeProperty): JCheckBox {
        isSelected = types.contains(value)
        addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                if (!types.contains(value)) types.add(value)
            } else types.remove(value)
        }
        return this
    }
}