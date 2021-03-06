package org.jb.cce.evaluation.step

import com.intellij.openapi.project.Project
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.highlighter.Highlighter
import org.jb.cce.util.Progress

class HighlightingTokensInIdeStep(project: Project, isHeadless: Boolean): BackgroundEvaluationStep(project, isHeadless) {
    override val name: String = "Highlighting tokens in IDE"

    override val description: String = "Highlight tokens on which completion was called"

    override fun runInBackground(workspace: EvaluationWorkspace, progress: Progress): EvaluationWorkspace {
        val highlighter = Highlighter(project)
        val sessionFiles = workspace.sessionsStorage.getSessionFiles()
        for (file in sessionFiles) {
            val sessionsInfo = workspace.sessionsStorage.getSessions(file.first)
            highlighter.highlight(sessionsInfo.sessions)
        }
        return workspace
    }
}