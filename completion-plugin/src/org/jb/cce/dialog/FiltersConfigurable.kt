package org.jb.cce.dialog

import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import com.intellij.util.EventDispatcher
import org.jb.cce.filter.EvaluationFilterConfiguration
import org.jb.cce.filter.EvaluationFilterManager
import org.jb.cce.uast.Language
import org.jb.cce.util.Config
import java.awt.FlowLayout
import java.awt.event.ItemEvent
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel

class FiltersConfigurable(private val dispatcher: EventDispatcher<SettingsListener>, private val initLanguage: String) : EvaluationConfigurable {
    private var completeAllTokens = false
    private var completeAllTokensPrev = false
    private val configurableMap: MutableMap<String, EvaluationFilterConfiguration.Configurable> = mutableMapOf()

    override fun createPanel(previousState: Config): JPanel {
        completeAllTokens = previousState.strategy.completeAllTokens
        val completeAllTokensPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Complete all tokens:"))
            add(JCheckBox("", completeAllTokens).configure())
        }
        val panel = panel(title = "Filters", constraints = *arrayOf(LCFlags.noGrid, LCFlags.flowY)) {}.apply { add(completeAllTokensPanel) }
        EvaluationFilterManager.getAllFilters().forEach {
            val configurable = it.createConfigurable(previousState.strategy.filters[it.id] ?: it.defaultFilter())
            configurableMap[it.id] = configurable
            panel.add(configurable.panel)
        }
        setFiltersByLanguage(initLanguage)
        return panel
    }

    override fun configure(builder: Config.Builder) {
        builder.allTokens = completeAllTokens
        if (!completeAllTokens) {
            for (entry in configurableMap) {
                builder.filters[entry.key] = entry.value.build()
            }
        }
    }

    private fun JCheckBox.configure(): JCheckBox {
        updateData(Language.resolve(initLanguage), this)
        dispatcher.multicaster.allTokensChanged(completeAllTokens)
        addItemListener {
            completeAllTokens = it.stateChange == ItemEvent.SELECTED
            setFilterPanelsEnabled(it.stateChange != ItemEvent.SELECTED)
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
            setFilterPanelsEnabled(false)
        } else {
            checkBox.isEnabled = true
            if (!completeAllTokensPrev) {
                completeAllTokens = false
                checkBox.isSelected = false
                setFilterPanelsEnabled(true)
                setFiltersByLanguage(language.displayName)
            }
        }
    }

    private fun setFiltersByLanguage(language: String) {
        configurableMap.forEach {
            if (!it.value.isLanguageSupported(language)) setPanelEnabled(it.value.panel, false)
            else if (!completeAllTokens) setPanelEnabled(it.value.panel, true)
        }
    }

    private fun setFilterPanelsEnabled(isEnabled: Boolean) = configurableMap.values.map { it.panel }.forEach { setPanelEnabled(it, isEnabled) }

    private fun setPanelEnabled(panel: JPanel, isEnabled: Boolean) {
        panel.isEnabled = isEnabled
        for (component in panel.components) {
            if (component is JPanel) {
                setPanelEnabled(component, isEnabled)
            }
            component.isEnabled = isEnabled
        }
    }
}
