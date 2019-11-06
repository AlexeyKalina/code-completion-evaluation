package org.jb.cce.dialog

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.EventDispatcher
import org.jb.cce.Config
import org.jb.cce.ConfigFactory
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
    private val properties = PropertiesComponent.getInstance(project)

    private val configurators: List<EvaluationConfigurable> = listOf(
            CompletionTypeConfigurable(),
            ContextConfigurable(dispatcher),
            PrefixConfigurable(),
            FiltersConfigurable(dispatcher, language),
            FilteringOnInterpretationConfigurable()
    )

    init {
        init()
        title = "Completion Evaluation Settings"
    }

    override fun createCenterPanel(): JComponent? {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            val value = properties.getValue(configStateKey)
            val previousState = try {
                if (value == null) ConfigFactory.defaultConfig(project.basePath!!)
                else ConfigFactory.deserialize(value)
            } catch (e: Throwable) {
                ConfigFactory.defaultConfig(project.basePath!!)
            }
            configurators.forEach { add(it.createPanel(previousState)) }
        }
    }

    fun buildConfig(): Config {
        val config = Config.build(project.basePath!!, language) {
            configurators.forEach { it.configure(this) }
            evaluationRoots = mutableListOf(path)
        }

        properties.setValue(configStateKey, ConfigFactory.serialize(config))

        return config
    }
}
