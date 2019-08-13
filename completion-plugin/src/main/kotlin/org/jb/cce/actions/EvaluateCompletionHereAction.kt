package org.jb.cce.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import org.jb.cce.CompletionEvaluator
import org.jb.cce.util.FilesHelper

class EvaluateCompletionHereAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val caret = e.getData(CommonDataKeys.CARET) ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val psi = psiFile.findElementAt(caret.offset) ?: return
        val language = FilesHelper.getLanguageByExtension(file.extension ?: "")
        if (language == null) {
            Messages.showInfoMessage(project, "Language of this file is't supported.", "Nothing to complete")
            return
        }

        val settingsDialog = CompletionSettingsDialog(project)
        val result = settingsDialog.showAndGet()
        if (!result) return

        val strategy = CompletionStrategy(settingsDialog.completionPrefix, settingsDialog.completionStatement, settingsDialog.completionContext)
        CompletionEvaluator(false).evaluateCompletionHere(project, file, language.displayName, caret.offset,
                if (strategy.statement == CompletionStatement.ALL_TOKENS) getParentOnSameLine(psi, caret.offset, editor) else null, strategy, settingsDialog.completionTypes)
    }

    private fun getParentOnSameLine(element: PsiElement, offset: Int, editor: Editor): PsiElement {
        val line = editor.offsetToLogicalPosition(offset).line
        var curElement = element
        var parent = element
        while (editor.offsetToLogicalPosition(curElement.textOffset).line == line) {
            parent = curElement
            curElement = curElement.parent
        }
        return parent
    }
}