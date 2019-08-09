package org.jb.cce.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.Messages
import org.jb.cce.CompletionEvaluator
import org.jb.cce.util.FilesHelper

class EvaluateCompletionHereAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val caret = e.getData(CommonDataKeys.CARET) ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val language = FilesHelper.getLanguageByExtension(file.extension ?: "")
        if (language == null) {
            Messages.showInfoMessage(project, "Language of this file is't supported.", "Nothing to complete")
            return
        }

        val settingsDialog = CompletionSettingsDialog(project)
        val result = settingsDialog.showAndGet()
        if (!result) return

        val strategy = CompletionStrategy(settingsDialog.completionPrefix, settingsDialog.completionStatement, settingsDialog.completionContext)
        CompletionEvaluator(false).evaluateCompletionHere(project, file, language.displayName, caret.offset, strategy, settingsDialog.completionTypes)
    }
}