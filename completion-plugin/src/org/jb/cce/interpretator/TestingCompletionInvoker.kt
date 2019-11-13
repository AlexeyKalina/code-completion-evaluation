package org.jb.cce.interpretator

import org.jb.cce.CompletionInvoker
import org.jb.cce.Lookup

class TestingCompletionInvoker(private val invoker: CompletionInvoker) : CompletionInvoker {
    override fun callCompletion(expectedText: String, prefix: String): Lookup {
        val lookup = invoker.callCompletion(expectedText, prefix)
        return Lookup(lookup.text, lookup.suggestions.take(1), 0)
    }

    override fun moveCaret(offset: Int) = invoker.moveCaret(offset)

    override fun finishCompletion(expectedText: String, prefix: String): Boolean = invoker.finishCompletion(expectedText, prefix)

    override fun printText(text: String) = invoker.printText(text)

    override fun deleteRange(begin: Int, end: Int) = invoker.deleteRange(begin, end)

    override fun openFile(file: String): String = invoker.openFile(file)

    override fun closeFile(file: String) = invoker.closeFile(file)

    override fun isOpen(file: String): Boolean = invoker.isOpen(file)

    override fun getText(): String = invoker.getText()
}