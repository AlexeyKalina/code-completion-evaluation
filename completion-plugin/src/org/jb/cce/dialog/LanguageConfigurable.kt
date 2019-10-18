package org.jb.cce.dialog

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.layout.CellBuilder
import com.intellij.ui.layout.panel
import com.intellij.util.EventDispatcher
import org.jb.cce.uast.Language
import org.jb.cce.util.Config
import java.awt.event.ItemEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.JLabel
import javax.swing.JPanel

class LanguageConfigurable(private val dispatcher: EventDispatcher<SettingsListener>,
                           private val language2files: Map<String, Set<VirtualFile>>) : EvaluationConfigurable {
    private val languages = language2files.map { LanguageItem(it.key, it.value.size) }.sortedByDescending { it.count }.toTypedArray()
    private var lang = languages[0]
    fun language(): String {
        return lang.languageName
    }

    override fun createPanel(previousState: Config): JPanel {
        return panel {
            row(JLabel("Language"), false) {
                comboBox(DefaultComboBoxModel(languages), { lang }, { }).configure()
            }
        }
    }

    override fun configure(builder: Config.Builder) {
        builder.evaluationRoots.addAll(language2files[language()]?.map { it.path } ?: emptyList())
    }

    private class LanguageItem(val languageName: String, val count: Int) {
        override fun toString(): String = "$languageName ($count)"
    }

    private fun CellBuilder<ComboBox<LanguageItem>>.configure() {
        component.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                lang = it.item as LanguageItem
                dispatcher.multicaster.languageChanged(Language.resolve(language()))
            }
        }
    }
}
