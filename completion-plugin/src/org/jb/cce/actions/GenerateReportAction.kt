package org.jb.cce.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.HtmlReportGenerator
import org.jb.cce.evaluation.ReportGenerationEvaluator
import org.jb.cce.util.Config
import org.jb.cce.util.ConfigFactory
import java.nio.file.Paths

class GenerateReportAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dirs = getFiles(e)
        val workspaces = mutableListOf<EvaluationWorkspace>()

        lateinit var config: Config
        for (workspaceDir in dirs) {
            config = ConfigFactory.load(Paths.get(workspaceDir.path, ConfigFactory.DEFAULT_CONFIG_NAME))
            workspaces.add(EvaluationWorkspace(workspaceDir.path, config.completionType, true).apply {
                sessionsStorage.evaluationTitle = config.evaluationTitle
            })
        }

        val workspace = EvaluationWorkspace(config.outputDir, config.completionType)
        val reportGenerator = HtmlReportGenerator(workspace.reportsDirectory())
        val evaluator = ReportGenerationEvaluator(reportGenerator, project, false)
        evaluator.generateReportUnderProgress(workspaces.map { it.sessionsStorage }, workspaces.map { it.errorsStorage })
    }

    override fun update(e: AnActionEvent) {
        val files = getFiles(e)
        e.presentation.isEnabled = files.isNotEmpty() && files.all { it.isDirectory && it.children.any {  it.name == ConfigFactory.DEFAULT_CONFIG_NAME } }
    }

    private fun getFiles(e: AnActionEvent) : List<VirtualFile> = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList() ?: emptyList()
}