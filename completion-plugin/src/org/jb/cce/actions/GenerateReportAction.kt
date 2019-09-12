package org.jb.cce.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.SessionSerializer
import org.jb.cce.SessionsStorage
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.generateReportUnderProgress
import java.nio.file.Paths

class GenerateReportAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = getFiles(e)
        val serializer = SessionSerializer()
        val storages = mutableListOf<SessionsStorage>()

        for (configFile in files) {
            val config = serializer.deserializeConfig(VfsUtil.loadText(configFile))
            storages.add(SessionsStorage(configFile.parent.path, config.evaluationType))
        }

        val properties = PropertiesComponent.getInstance(project)
        val workspaceDir = properties.getValue(CompletionSettingsDialog.workspaceDirProperty) ?:
            Paths.get(project.basePath ?: "", CompletionSettingsDialog.completionEvaluationDir).toString()

        val workspace = EvaluationWorkspace(workspaceDir)
        generateReportUnderProgress(workspace, storages, null, project, false)
    }

    override fun update(e: AnActionEvent) {
        val files = getFiles(e)
        e.presentation.isEnabled = files.isNotEmpty() && files.all { it.extension == "json" }
    }

    private fun getFiles(e: AnActionEvent) : List<VirtualFile> = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList() ?: emptyList()
}