package org.jb.cce.dialog.configurable

import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.Row
import org.jb.cce.filter.ConfigurableBuilder
import org.jb.cce.filter.EvaluationFilterManager
import org.jb.cce.filter.impl.ArgumentFilterConfiguration
import org.jb.cce.filter.impl.PackageRegexFilterConfiguration
import org.jb.cce.filter.impl.StaticFilterConfiguration
import org.jb.cce.filter.impl.TypeFilterConfiguration
import org.jb.cce.util.Config

class FilterUIConfigurableFactory(private val previousState: Config, private val layout: LayoutBuilder) : ConfigurableBuilder<Row> {
    override fun build(filterId: String): UIConfigurable {
        val previousFilterState = previousState.strategy.filters[filterId]
                ?: EvaluationFilterManager.getConfigurationById(filterId)?.defaultFilter()
                ?: throw IllegalArgumentException("Unknown filter id: $filterId")
        return when (filterId) {
            TypeFilterConfiguration.id -> TypeUIConfigurable(previousFilterState, layout)
            ArgumentFilterConfiguration.id -> ArgumentUIConfigurable(previousFilterState, layout)
            StaticFilterConfiguration.id -> StaticUIConfigurable(previousFilterState, layout)
            PackageRegexFilterConfiguration.id -> PackageRegexUIConfigurable(previousFilterState, layout)
            else -> throw IllegalStateException("UI configuration is not supported for filter id = $filterId")
        }
    }
}