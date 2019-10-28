package org.jb.cce.dialog

import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import com.intellij.util.EventDispatcher
import org.jb.cce.dialog.configurable.FilterUIConfigurableFactory
import org.jb.cce.dialog.configurable.UIConfigurable
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.filter.EvaluationFilterManager
import org.jb.cce.uast.Language
import org.jb.cce.util.Config
import java.awt.event.ItemEvent
import javax.swing.JCheckBox
import javax.swing.JPanel

class FiltersConfigurable(private val dispatcher: EventDispatcher<SettingsListener>, private val initLanguage: String) : EvaluationConfigurable {
    private var completeAllTokens = false
    private var completeAllTokensPrev = false
    private val configurableMap: MutableMap<String, UIConfigurable> = mutableMapOf()

    override fun createPanel(previousState: Config): JPanel {
        completeAllTokens = previousState.strategy.completeAllTokens
        val panel = panel(title = "Filters") {
            row {
                cell {
                    label("Complete all tokens:")
                    checkBox("", completeAllTokens).configure()
                }
            }
            val provider = FilterUIConfigurableFactory(previousState, this)
            for (filter in EvaluationFilterManager.getAllFilters()) {
                getFilterView(filter, provider)
            }
        }
        setFiltersByLanguage(initLanguage)
        return panel
    }

    private fun getFilterView(filter: EvaluationFilterConfiguration, provider: FilterUIConfigurableFactory): Row {
        val configurable = provider.build(filter.id)
        configurableMap[filter.id] = configurable
        return configurable.view
    }

    override fun configure(builder: Config.Builder) {
        builder.allTokens = completeAllTokens
        if (!completeAllTokens) {
            for (entry in configurableMap) {
                if (entry.value.view.enabled)
                    builder.filters[entry.key] = entry.value.build()
            }
        }
    }

    private fun JCheckBox.configure(): JCheckBox {
        updateData(Language.resolve(initLanguage), this)
        dispatcher.multicaster.allTokensChanged(completeAllTokens)
        addItemListener {
            completeAllTokens = it.stateChange == ItemEvent.SELECTED
            setFilterViewsEnabled(it.stateChange != ItemEvent.SELECTED)
            dispatcher.multicaster.allTokensChanged(it.stateChange == ItemEvent.SELECTED)
        }
        dispatcher.addListener(object : SettingsListener {
            override fun languageChanged(language: Language) = updateData(language, this@configure)
        })
        return this
    }

    private fun updateData(language: Language, checkBox: JCheckBox) {
        if (language == Language.ANOTHER) {
            completeAllTokensPrev = completeAllTokens
            completeAllTokens = true
            checkBox.isSelected = true
            checkBox.isEnabled = false
            setFilterViewsEnabled(false)
        } else {
            checkBox.isEnabled = true
            if (!completeAllTokensPrev) {
                completeAllTokens = false
                checkBox.isSelected = false
                setFilterViewsEnabled(true)
                setFiltersByLanguage(language.displayName)
            }
        }
    }

    private fun setFiltersByLanguage(language: String) {
        configurableMap.forEach {
            if (!EvaluationFilterManager.getConfigurationById(it.key)!!.isLanguageSupported(language)) setViewEnabled(it.value.view, false)
            else if (!completeAllTokens) setViewEnabled(it.value.view, true)
        }
    }

    private fun setFilterViewsEnabled(isEnabled: Boolean) = configurableMap.values.forEach { setViewEnabled(it.view, isEnabled) }

    private fun setViewEnabled(view: Row, isEnabled: Boolean) {
        view.enabled = isEnabled
        view.subRowsEnabled = isEnabled
    }
}
