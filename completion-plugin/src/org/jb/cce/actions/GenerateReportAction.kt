package org.jb.cce.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.HtmlReportGenerator
import org.jb.cce.ReportGeneration
import org.jb.cce.SessionSerializer
import org.jb.cce.dialog.FullSettingsDialog
import org.jb.cce.util.ConfigFactory

class GenerateReportAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = getFiles(e)
        val serializer = SessionSerializer()
        val workspaces = mutableListOf<EvaluationWorkspace>()

        for (configFile in files) {
            val config = serializer.deserializeConfig(VfsUtil.loadText(configFile))
            workspaces.add(EvaluationWorkspace(configFile.parent.parent.path, config.evaluationType, true))
        }

        val config = ConfigFactory.getByKey(project, FullSettingsDialog.configStateKey)
        val workspace = EvaluationWorkspace(config.workspaceDir, "COMPARE_MULTIPLE")
        val reportGenerator = HtmlReportGenerator(workspace.reportsDirectory())
        ReportGeneration(reportGenerator).generateReportUnderProgress(workspaces.map { it.sessionsStorage }, workspaces.map { it.errorsStorage }, project, false)
    }

    override fun update(e: AnActionEvent) {
        val files = getFiles(e)
        e.presentation.isEnabled = files.isNotEmpty() && files.all { it.extension == "json" }
    }

    private fun getFiles(e: AnActionEvent) : List<VirtualFile> = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList() ?: emptyList()
}