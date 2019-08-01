package org.jb.cce.interpretator

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import org.jb.cce.CompletionInvoker
import org.jb.cce.Suggestion
import java.io.File
import kotlin.system.measureTimeMillis

class CompletionInvokerImpl(private val project: Project) : CompletionInvoker {
    private companion object {
        val LOG = Logger.getInstance(CompletionInvokerImpl::class.java)
        const val LOG_MAX_LENGTH = 50
        const val WAITING_TIME = 100L
    }

    private var editor: Editor? = null

    override fun moveCaret(offset: Int) {
        LOG.info("Move caret. ${positionToString(offset)}")
        editor!!.caretModel.moveToOffset(offset)
        editor!!.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }

    override fun callCompletion(type: org.jb.cce.actions.CompletionType, expectedText: String, prefix: String): org.jb.cce.Lookup {
        LOG.info("Call completion. Type: $type. ${positionToString(editor!!.caretModel.offset)}")
        LookupManager.getInstance(project).hideActiveLookup()
        val completionType = when (type) {
            org.jb.cce.actions.CompletionType.BASIC -> CompletionType.BASIC
            org.jb.cce.actions.CompletionType.SMART -> CompletionType.SMART
            org.jb.cce.actions.CompletionType.ML -> CompletionType.BASIC
        }

        var latency = measureTimeMillis {
            CodeCompletionHandlerBase(completionType, false, false, true).invokeCompletion(project, editor)
        }
        if (LookupManager.getActiveLookup(editor) == null) {
            return org.jb.cce.Lookup(prefix, emptyList(), false, latency)
        } else {
            val lookup = LookupManager.getActiveLookup(editor) as LookupImpl
            latency += measureTimeMillis {
                lookup.waitForResult(1000)
            }
            val expectedItemIndex = lookup.items.indexOfFirst { it.lookupString == expectedText }
            if (expectedItemIndex != -1 && completionType != CompletionType.SMART) {
                lookup.selectedIndex = expectedItemIndex
                lookup.finishLookup(Lookup.AUTO_INSERT_SELECT_CHAR, lookup.items[expectedItemIndex])
            }
            val suggests = lookup.items.map { Suggestion(it.lookupString, lookupElementText(it)) }
            return org.jb.cce.Lookup(prefix, suggests, expectedItemIndex != -1, latency)
        }
    }

    override fun printText(text: String) {
        LOG.info("Print text: ${StringUtil.shortenPathWithEllipsis(text, LOG_MAX_LENGTH)}. ${positionToString(editor!!.caretModel.offset)}")
        val document = editor!!.document
        val project = editor!!.project
        val initialOffset = editor!!.caretModel.offset
        val runnable = Runnable { document.insertString(initialOffset, text) }
        WriteCommandAction.runWriteCommandAction(project, runnable)
        editor!!.caretModel.moveToOffset(initialOffset + text.length)
    }

    override fun deleteRange(begin: Int, end: Int) {
        val document = editor!!.document
        val textForDelete = StringUtil.shortenPathWithEllipsis(document.text.substring(begin, end), LOG_MAX_LENGTH)
        LOG.info("Delete range. Text: $textForDelete. Begin: ${positionToString(begin)} End: ${positionToString(end)}")
        val project = editor!!.project
        val runnable = Runnable { document.deleteString(begin, end) }
        WriteCommandAction.runWriteCommandAction(project, runnable)
    }

    override fun openFile(file: String) {
        LOG.info("Open file: $file")
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(file))
        val descriptor = OpenFileDescriptor(project, virtualFile!!)
        val fileEditor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
                ?: throw Exception("Can't open text editor for file: $file")
        editor = fileEditor
    }

    override fun closeFile(file: String) {
        LOG.info("Close file: $file")
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(file))
        FileEditorManager.getInstance(project).closeFile(virtualFile!!)
        editor = null
    }

    override fun isOpen(file: String): Boolean {
        return FileEditorManager.getInstance(project).openFiles.any { it.path == file }
    }

    private fun positionToString(offset: Int): String {
        val logicalPosition = editor!!.offsetToLogicalPosition(offset)
        return "Offset: $offset, Line: ${logicalPosition.line}, Column: ${logicalPosition.column}."
    }

    private fun lookupElementText(element: LookupElement): String {
        val presentation = LookupElementPresentation()
        element.renderElement(presentation)
        return "${presentation.itemText} ${presentation.typeText} ${presentation.tailText}"
    }

    private fun LookupImpl.waitForResult(timeMs: Long): Boolean {
        var totalWaitingTimeMs = 0L
        while (isCalculating && totalWaitingTimeMs < timeMs) {
            Thread.sleep(WAITING_TIME)
            totalWaitingTimeMs += WAITING_TIME
        }

        return !isCalculating
    }
}