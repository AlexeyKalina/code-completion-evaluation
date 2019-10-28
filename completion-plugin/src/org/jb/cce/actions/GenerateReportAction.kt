package org.jb.cce.actions

import com.google.gson.Gson
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.HtmlReportGenerator
import org.jb.cce.ReportGeneration
import org.jb.cce.dialog.FullSettingsDialog
import org.jb.cce.info.EvaluationInfo
import org.jb.cce.util.ConfigFactory

class GenerateReportAction : AnAction() {
    companion object {
        private val gson = Gson()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = getFiles(e)
        val workspaces = mutableListOf<EvaluationWorkspace>()

        for (configFile in files) {
            val evaluationInfo = gson.fromJson<EvaluationInfo>(VfsUtil.loadText(configFile), EvaluationInfo::class.java)
            workspaces.add(EvaluationWorkspace(configFile.parent.parent.path, evaluationInfo.evaluationType, true))
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