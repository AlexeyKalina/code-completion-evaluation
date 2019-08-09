package org.jb.cce.highlighter

import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.codeInsight.hints.presentation.SpacePresentation
import com.intellij.codeInsight.lookup.LookupArranger
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import org.jb.cce.ColorizeTokens
import org.jb.cce.Session
import org.jb.cce.info.SessionsEvaluationInfo
import java.awt.Color
import java.awt.Font
import java.io.File

class Highlighter(private val project: Project): ColorizeTokens<Color> {
    override val middleCountLookups = 3
    override val absentColor = Color(112, 170, 255)
    override val goodColor = Color(188, 245, 188)
    override val middleColor = Color(255, 250, 205)
    override val badColor = Color(255, 153, 153)

    fun highlight(evaluationResults: List<SessionsEvaluationInfo>) {
        val filePath = evaluationResults.first().sessions.first().filePath
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(filePath))
        var fileEditor: TextEditor? = null
        ApplicationManager.getApplication().invokeAndWait {
            val fileEditorManager = FileEditorManager.getInstance(project)
            if (fileEditorManager.openFiles.any { it.path == filePath })
                fileEditor = FileEditorManager.getInstance(project).openFile(virtualFile!!, false)[0] as TextEditor
        }
        val editor = fileEditor?.editor ?: return
        val delimiterRenderer = PresentationRenderer(SpacePresentation(3, editor.lineHeight))

        val sessions = evaluationResults.map { it.sessions.first().results }
        val offsets = sessions.flatten().map { it.offset }.distinct().sorted()
        val sessionGroups = offsets.map { offset -> sessions.map { it.find { session -> session.offset == offset } } }
        ApplicationManager.getApplication().invokeAndWait {
            editor.markupModel.removeAllHighlighters()
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
    }

    private fun addHighlight(editor: Editor, session: Session?, begin: Int, end: Int) {
        val color = getColor(session)
        editor.markupModel.addRangeHighlighter(begin, end, HighlighterLayer.LAST,
                TextAttributes(null, color, null, EffectType.BOXED, Font.PLAIN), HighlighterTargetArea.EXACT_RANGE)

        editor.addEditorMouseListener(object : EditorMouseListener {
            override fun mouseClicked(event: EditorMouseEvent) {
                if (editor.caretModel.offset in begin+1 .. end) {
                    val lookup = LookupManager.getInstance(project).createLookup(editor, LookupElement.EMPTY_ARRAY, "",
                            LookupArranger.DefaultArranger()) as LookupImpl
                    for (completion in session?.lookups?.last()?.suggestions ?: emptyList()) {
                        val item = if (completion.text == session!!.expectedText)
                            LookupElementBuilder.create(completion.presentationText).bold()
                        else LookupElementBuilder.create(completion.presentationText)
                        lookup.addItem(item, PrefixMatcher.ALWAYS_TRUE)
                    }
                    lookup.showLookup()
                }
            }
        })
    }
}
