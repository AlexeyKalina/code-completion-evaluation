package org.jb.cce.dialog

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.EventDispatcher
import org.jb.cce.Config
import org.jb.cce.ConfigFactory
import org.jb.cce.util.FilesHelper
import java.awt.event.ActionEvent
import javax.swing.*

class FullSettingsDialog(
        private val project: Project,
        private val files: List<VirtualFile>,
        language2files: Map<String, Set<VirtualFile>>
) : DialogWrapper(true) {
    companion object {
        const val configStateKey = "org.jb.cce.config.full"
    }
    private val properties = PropertiesComponent.getInstance(project)
    private val dispatcher = EventDispatcher.create(SettingsListener::class.java)
    private val languageConfigurable = LanguageConfigurable(dispatcher, language2files)

    private val configurators: List<EvaluationConfigurable> = listOf(
            languageConfigurable,
            CompletionTypeConfigurable(),
            ContextConfigurable(),
            PrefixConfigurable(),
            FiltersConfigurable(dispatcher, languageConfigurable.language()),
            FlowConfigurable()
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

    override fun createLeftSideActions(): Array<Action> {
        return super.createLeftSideActions() + listOf(object : DialogWrapperAction("Save config") {
            override fun doAction(e: ActionEvent?) {
                val fileChooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = "Choose directory for new config"
                }
                val result = fileChooser.showOpenDialog(contentPanel)
                if (result == JFileChooser.APPROVE_OPTION) {
                    val config = buildConfig()
                    ConfigFactory.save(config, fileChooser.selectedFile.toPath())
                    Messages.showInfoMessage("Config saved successfully", "Config saving")
                }
            }
        })
    }

    fun buildConfig(): Config {
        val config = Config.build(project.basePath!!, languageConfigurable.language()) {
            configurators.forEach { it.configure(this) }
            evaluationRoots = files.map { FilesHelper.getRelativeToProjectPath(project, it.path) }.toMutableList()
        }

        properties.setValue(EvaluateHereSettingsDialog.configStateKey, ConfigFactory.serialize(config))

        return config
    }
}