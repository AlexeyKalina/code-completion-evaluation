package org.jb.cce

import org.jb.cce.actions.CompletionType

interface CompletionInvoker {
    fun moveCaret(offset: Int)
    fun callCompletion(type: CompletionType, expectedText: String): List<Suggest>
    fun printText(text: String)
    fun deleteRange(begin: Int, end: Int)
    fun openFile(file: String)
    fun closeFile(file: String)
    fun isOpen(file: String): Boolean
}