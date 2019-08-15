package org.jb.cce.highlighter

import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.codeInsight.hints.presentation.SpacePresentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jb.cce.ReportColors.Companion.getColor
import org.jb.cce.Session
import org.jb.cce.info.SessionsEvaluationInfo
import java.awt.Font

class Highlighter(private val project: Project) {
    companion object {
        private val listenerKey = Key<HighlightersClickListener>("org.jb.cce.highlighter.listener")
    }

    private lateinit var listener : HighlightersClickListener

    fun highlight(evaluationResults: List<SessionsEvaluationInfo>) {
        val editor = (FileEditorManager.getInstance(project).selectedEditors[0] as TextEditor).editor
        val delimiterRenderer = PresentationRenderer(SpacePresentation(3, editor.lineHeight))
        listener = editor.getUserData(listenerKey) ?: HighlightersClickListener(editor, project)
        editor.addEditorMouseListener(listener)

        val sessions = evaluationResults.map { it.sessions.first().results }
        val offsets = sessions.flatten().map { it.offset }.distinct().sorted()
        val sessionGroups = offsets.map { offset -> sessions.map { it.find { session -> session.offset == offset } } }
        ApplicationManager.getApplication().invokeAndWait {
            editor.markupModel.removeAllHighlighters()
            listener.clear()
            for (sessionGroup in sessionGroups) {
                val session = sessionGroup.filterNotNull().first()
                val center = session.expectedText.length / sessions.size
                var shift = 0
                for (j in 0 until sessionGroup.lastIndex) {
                    addHighlight(editor, sessionGroup[j], session.offset + shift, session.offset + shift + center)
                    editor.inlayModel.addInlineElement(session.offset + shift + center, delimiterRenderer)
                    shift += center
                }
                addHighlight(editor, sessionGroup.last(), session.offset + shift, session.offset + session.expectedText.length)
            }
        }
        editor.putUserData(listenerKey, listener)
    }

    private fun addHighlight(editor: Editor, session: Session?, begin: Int, end: Int) {
        val color = getColor(session, HighlightColors)
        editor.markupModel.addRangeHighlighter(begin, end, HighlighterLayer.LAST,
                TextAttributes(null, color, null, EffectType.BOXED, Font.PLAIN), HighlighterTargetArea.EXACT_RANGE)
        if (session != null) listener.addSession(session, begin, end)
    }
}
