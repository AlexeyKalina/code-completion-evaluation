package org.jb.cce.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.ConfigFactory
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.evaluation.BackgroundStepFactory
import org.jb.cce.evaluation.EvaluationProcess
import org.jb.cce.evaluation.EvaluationRootInfo

class GenerateReportAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dirs = getFiles(e)
        val config = dirs.map { EvaluationWorkspace.open(it.path) }.buildMultipleEvaluationsConfig()
        val outputWorkspace = EvaluationWorkspace.create(config)
        val process = EvaluationProcess.build({
            shouldGenerateReports = true
        }, BackgroundStepFactory(config, project, false, dirs.map { it.path }, EvaluationRootInfo(true)))
        process.startAsync(outputWorkspace)
    }

    override fun update(e: AnActionEvent) {
        val files = getFiles(e)
        e.presentation.isEnabled = files.isNotEmpty() && files.all { it.isDirectory && it.children.any {  it.name == ConfigFactory.DEFAULT_CONFIG_NAME } }
    }

    private fun getFiles(e: AnActionEvent) : List<VirtualFile> = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList() ?: emptyList()
}