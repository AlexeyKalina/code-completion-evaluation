package org.jb.cce.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.dialog.FullSettingsDialog
import org.jb.cce.evaluation.*
import org.jb.cce.util.FilesHelper

class EvaluateCompletionForSelectedFilesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList() ?: emptyList<VirtualFile>()

        val language2files = FilesHelper.getFiles(project, files)
        if (language2files.isEmpty()) {
            Messages.showInfoMessage(project, "Languages of selected files aren't supported.", "Nothing to complete")
            return
        }

        val dialog = FullSettingsDialog(project, files, language2files)
        val result = dialog.showAndGet()
        if (!result) return

        val config = dialog.buildConfig()
        val workspace = EvaluationWorkspace.create(config)
        val process = EvaluationProcess.build({
            shouldGenerateActions = true
            shouldInterpretActions = true
            shouldGenerateReports = true
        }, BackgroundStepFactory(config, project, false, null, EvaluationRootInfo(true)))
        process.startAsync(workspace)
    }
}