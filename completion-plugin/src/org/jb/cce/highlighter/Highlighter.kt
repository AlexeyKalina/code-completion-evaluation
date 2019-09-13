package org.jb.cce.highlighter

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
import java.awt.Font

class Highlighter(private val project: Project) {
    companion object {
        private val listenerKey = Key<HighlightersClickListener>("org.jb.cce.highlighter.listener")
    }

    private lateinit var listener : HighlightersClickListener

    fun highlight(sessions: List<Session>) {
        val editor = (FileEditorManager.getInstance(project).selectedEditors[0] as TextEditor).editor
        listener = editor.getUserData(listenerKey) ?: HighlightersClickListener(editor, project)
        editor.addEditorMouseListener(listener)

        val offsets = sessions.map { it.offset }.distinct().sorted()
        ApplicationManager.getApplication().invokeLater {
            editor.markupModel.removeAllHighlighters()
            listener.clear()
            for (session in sessions) {
                addHighlight(editor, session, session.offset, session.offset + session.expectedText.length)
            }
            editor.putUserData(listenerKey, listener)
        }
    }

    private fun addHighlight(editor: Editor, session: Session, begin: Int, end: Int) {
        val color = getColor(session, HighlightColors)
        editor.markupModel.addRangeHighlighter(begin, end, HighlighterLayer.LAST,
                TextAttributes(null, color, null, EffectType.BOXED, Font.PLAIN), HighlighterTargetArea.EXACT_RANGE)
        listener.addSession(session, begin, end)
    }
}
