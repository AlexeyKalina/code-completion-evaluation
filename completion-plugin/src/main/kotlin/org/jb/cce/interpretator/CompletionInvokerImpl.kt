package org.jb.cce.interpretator

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionProgressIndicator
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import org.jb.cce.CompletionInvoker
import java.io.File

class CompletionInvokerImpl(private val project: Project) : CompletionInvoker {
    private companion object {
        val LOG = Logger.getInstance(CompletionInvokerImpl::class.java)
        val LOG_MAX_LENGTH = 50
    }
    private var editor: Editor? = null

    override fun moveCaret(offset: Int) {
        ApplicationManager.getApplication().invokeAndWait {
            runWriteAction {
                LOG.info("Move caret. ${positionToString(offset)}")
                editor!!.caretModel.moveToOffset(offset)
                editor!!.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
            }
        }
    }

    override fun callCompletion(type: org.jb.cce.actions.CompletionType): List<String> {
        lateinit var result: List<String>
        ApplicationManager.getApplication().invokeAndWait {
            runReadAction {
                LOG.info("Call completion. Type: $type. ${positionToString(editor!!.caretModel.offset)}")
                LookupManager.getInstance(project).hideActiveLookup()
                val completionType = when (type) {
                    org.jb.cce.actions.CompletionType.BASIC -> CompletionType.BASIC
                    org.jb.cce.actions.CompletionType.SMART -> CompletionType.SMART
                }

                CodeCompletionHandlerBase(completionType, false, false, true).invokeCompletion(project, editor)
                if (LookupManager.getActiveLookup(editor) == null) {
                    result = ArrayList()
                } else {
                    val lookup = LookupManager.getActiveLookup(editor) as LookupImpl
                    result = lookup.items.toTypedArray().map(LookupElement::toString).toList()
                }
            }
        }
        return result
    }

    override fun printText(text: String) {
        ApplicationManager.getApplication().invokeAndWait {
            runWriteAction {
                LOG.info("Print text: ${StringUtil.shortenPathWithEllipsis(text, LOG_MAX_LENGTH)}. ${positionToString(editor!!.caretModel.offset)}")
                val document = editor!!.document
                val project = editor!!.project
                val initialOffset = editor!!.caretModel.offset
                val runnable = Runnable { document.insertString(initialOffset, text) }
                WriteCommandAction.runWriteCommandAction(project, runnable)
                editor!!.caretModel.moveToOffset(initialOffset + text.length)
            }
        }
    }

    override fun deleteRange(begin: Int, end: Int) {
        ApplicationManager.getApplication().invokeAndWait {
            runWriteAction {
                val document = editor!!.document
                val textForDelete = StringUtil.shortenPathWithEllipsis(document.text.substring(begin, end), LOG_MAX_LENGTH)
                LOG.info("Delete range. Text: $textForDelete. Begin: ${positionToString(begin)} End: ${positionToString(end)}")
                val project = editor!!.project
                val runnable = Runnable { document.deleteString(begin, end) }
                WriteCommandAction.runWriteCommandAction(project, runnable)
            }
        }
    }

    override fun openFile(file: String) {
        ApplicationManager.getApplication().invokeAndWait {
            runWriteAction {
                LOG.info("Open file: $file")
                val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(file))
                val fileEditor = FileEditorManager.getInstance(project).openFile(virtualFile!!, false)[0]
                editor = (fileEditor as TextEditor).editor
            }
        }
    }

    override fun closeFile(file: String) {
        ApplicationManager.getApplication().invokeAndWait {
            runWriteAction {
                LOG.info("Close file: $file")
                val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(file))
                FileEditorManager.getInstance(project).closeFile(virtualFile!!)
                editor = null
            }
        }
    }

    private fun positionToString(offset: Int): String {
        lateinit var logicalPosition: LogicalPosition
        ApplicationManager.getApplication().invokeAndWait {
            runReadAction {
                logicalPosition = editor!!.offsetToLogicalPosition(offset)
            }
        }
        return "Offset: $offset, Line: ${logicalPosition.line}, Column: ${logicalPosition.column}."
    }
}