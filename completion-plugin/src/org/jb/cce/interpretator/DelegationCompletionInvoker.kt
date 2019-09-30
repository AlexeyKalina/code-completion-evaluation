package org.jb.cce.interpretator

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import org.jb.cce.CompletionInvoker
import org.jb.cce.Lookup
import org.jb.cce.util.ListSizeRestriction

class DelegationCompletionInvoker(private val invoker: CompletionInvoker, project: Project) : CompletionInvoker {
    private val applicationListenersRestriction = ListSizeRestriction.applicationListeners()
    private val dumbService = DumbService.getInstance(project)

    override fun moveCaret(offset: Int) = onEdt {
        invoker.moveCaret(offset)
    }

    override fun callCompletion(expectedText: String, prefix: String): Lookup {
        applicationListenersRestriction.waitForSize(100)
        return readAction {
            invoker.callCompletion(expectedText, prefix)
        }
    }

    override fun finishCompletion(expectedText: String, prefix: String) = readAction {
        invoker.finishCompletion(expectedText, prefix)
    }

    override fun printText(text: String) = writeAction {
        invoker.printText(text)
    }

    override fun deleteRange(begin: Int, end: Int) = writeAction {
        invoker.deleteRange(begin, end)
    }

    override fun openFile(file: String): String = readAction {
        invoker.openFile(file)
    }

    override fun closeFile(file: String) = onEdt {
        invoker.closeFile(file)
    }

    override fun isOpen(file: String) = readAction {
        invoker.isOpen(file)
    }

    override fun getText(): String = readAction {
        invoker.getText()
    }

    private fun <T> readAction(runnable: () -> T): T {
        var result: T? = null
        ApplicationManager.getApplication().invokeAndWait {
            result = dumbService.runReadActionInSmartMode<T>(runnable)
        }

        return result!!
    }

    private fun writeAction(action: () -> Unit) {
        ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction(action)
        }
    }

    private fun onEdt(action: () -> Unit) = ApplicationManager.getApplication().invokeAndWait {
        action()
    }
}