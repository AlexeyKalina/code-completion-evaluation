package org.jb.cce.dialog.configurable

import com.intellij.ui.layout.LayoutBuilder
import org.jb.cce.filter.ConfigurableBuilder
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.filter.impl.ArgumentFilterConfiguration
import org.jb.cce.filter.impl.PackageRegexFilterConfiguration
import org.jb.cce.filter.impl.StaticFilterConfiguration
import org.jb.cce.filter.impl.TypeFilterConfiguration
import org.jb.cce.util.Config

class UIConfigurableBuilder(private val id: String, private val previousState: Config, private val layout: LayoutBuilder): ConfigurableBuilder {
    override fun build(): EvaluationFilterConfiguration.Configurable {
        val previousFilterState = previousState.strategy.filters.getValue(id)
        return when (id) {
            TypeFilterConfiguration.id -> TypeUIConfigurable(previousFilterState, layout)
            ArgumentFilterConfiguration.id -> ArgumentUIConfigurable(previousFilterState, layout)
            StaticFilterConfiguration.id -> StaticUIConfigurable(previousFilterState, layout)
            PackageRegexFilterConfiguration.id -> PackageRegexUIConfigurable(previousFilterState, layout)
            else -> throw IllegalArgumentException("Unknown filter id: $id")
        }
    }
}