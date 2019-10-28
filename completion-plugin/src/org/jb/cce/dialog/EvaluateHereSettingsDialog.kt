package org.jb.cce.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.EventDispatcher
import org.jb.cce.util.Config
import org.jb.cce.util.ConfigFactory
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

class EvaluateHereSettingsDialog(
        private val project: Project,
        private val language: String,
        private val path: String
) : DialogWrapper(true) {
    companion object {
        const val configStateKey = "org.jb.cce.config.evaluate_here"
    }
    private val dispatcher = EventDispatcher.create(SettingsListener::class.java)

    private val configurators: List<EvaluationConfigurable> = listOf(
            CompletionTypeConfigurable(),
            ContextConfigurable(dispatcher),
            PrefixConfigurable(),
            FiltersConfigurable(dispatcher, language)
    )

    init {
        init()
        title = "Completion Evaluation Settings"
    }

    override fun createCenterPanel(): JComponent? {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            val defaultState = ConfigFactory.getByKey(project, configStateKey)
            configurators.forEach { add(it.createPanel(defaultState)) }
        }
    }

    fun buildConfig(): Config {
        val config = Config.build(project.basePath!!, language) {
            configurators.forEach { it.configure(this) }
            evaluationRoots = mutableListOf(path)
        }

        ConfigFactory.storeByKey(project, configStateKey, config)

        return config
    }
}
