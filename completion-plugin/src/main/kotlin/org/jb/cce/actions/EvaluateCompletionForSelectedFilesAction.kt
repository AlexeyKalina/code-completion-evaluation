package org.jb.cce.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.CompletionEvaluator
import org.jb.cce.util.FilesHelper

class EvaluateCompletionForSelectedFilesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList() ?: emptyList<VirtualFile>()

        val evaluator = CompletionEvaluator(false)

        val language2files = FilesHelper.getFiles(files)
        if (language2files.isEmpty()) {
            Messages.showInfoMessage(project, "Languages of selected files aren't supported.", "Nothing to complete")
            return
        }

        val settingsDialog = CompletionSettingsDialog(project, language2files)
        val result = settingsDialog.showAndGet()
        if (!result) return

        val strategy = CompletionStrategy(settingsDialog.completionPrefix, settingsDialog.completionStatement, settingsDialog.completionContext)
        val completionTypes = settingsDialog.completionTypes
        evaluator.evaluateCompletion(project, files, settingsDialog.language, strategy, completionTypes, settingsDialog.workspaceDir,
                settingsDialog.interpretActionsAfterGeneration, settingsDialog.saveLogs, settingsDialog.trainingPercentage)
    }
}