package org.jb.cce.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.evaluation.BackgroundStepFactory
import org.jb.cce.evaluation.EvaluationProcess
import org.jb.cce.evaluation.EvaluationRootInfo
import org.jb.cce.util.ConfigFactory
import java.nio.file.Paths

class GenerateReportAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dirs = getFiles(e)
        val config = ConfigFactory.load(Paths.get(dirs.first().path, ConfigFactory.DEFAULT_CONFIG_NAME))
        val workspace = EvaluationWorkspace(config.outputDir)
        val process = EvaluationProcess.build({ this.apply {
            this.shouldGenerateReports = true
        } }, BackgroundStepFactory(config, project, false, dirs.map { it.path }, EvaluationRootInfo(true)))
        process.start(workspace)
    }

    override fun update(e: AnActionEvent) {
        val files = getFiles(e)
        e.presentation.isEnabled = files.isNotEmpty() && files.all { it.isDirectory && it.children.any {  it.name == ConfigFactory.DEFAULT_CONFIG_NAME } }
    }

    private fun getFiles(e: AnActionEvent) : List<VirtualFile> = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList() ?: emptyList()
}