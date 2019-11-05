package org.jb.cce.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.dialog.EvaluateHereSettingsDialog
import org.jb.cce.evaluation.BackgroundStepFactory
import org.jb.cce.evaluation.EvaluationProcess
import org.jb.cce.evaluation.EvaluationRootInfo
import org.jb.cce.util.ConfigFactory
import org.jb.cce.util.FilesHelper

class EvaluateCompletionHereAction : AnAction() {
    private companion object {
        val LOG = Logger.getInstance(EvaluateCompletionHereAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return LOG.error("Project is null.")
        val caret = e.getData(CommonDataKeys.CARET) ?: return LOG.error("No value for key ${CommonDataKeys.CARET}.")
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return LOG.error("No value for key ${CommonDataKeys.EDITOR}.")
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return LOG.error("No value for key ${CommonDataKeys.VIRTUAL_FILE}.")
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return LOG.error("No value for key ${CommonDataKeys.PSI_FILE}.")
        val psi = psiFile.findElementAt(caret.offset) ?: return LOG.error("No psi element under caret.")
        val language = FilesHelper.getLanguageByExtension(file.extension ?: "")
        if (language == null) {
            Messages.showInfoMessage(project, "Language of this file is't supported.", "Nothing to complete")
            return
        }

        val settingsDialog = EvaluateHereSettingsDialog(project, language.displayName, file.path)
        val result = settingsDialog.showAndGet()
        if (!result) return
        val config = settingsDialog.buildConfig()
        val workspace = EvaluationWorkspace(config.outputDir)
        ConfigFactory.save(config, workspace.path())
        val parentPsiElement = if (config.actions.strategy.completeAllTokens) getParentOnSameLine(psi, caret.offset, editor) else null
        val process = EvaluationProcess.build({
            shouldGenerateActions = true
            shouldInterpretActions = true
            shouldHighlightInIde = true
        }, BackgroundStepFactory(config, project, false, null, EvaluationRootInfo(false, caret.offset, parentPsiElement)))
        process.startAsync(workspace)
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