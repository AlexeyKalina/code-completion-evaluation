package org.jb.cce

interface CompletionInvoker {
    fun moveCaret(offset: Int)
    fun callCompletion(expectedText: String, prefix: String): Lookup
    fun finishCompletion(expectedText: String, prefix: String): Boolean
    fun printText(text: String)
    fun deleteRange(begin: Int, end: Int)
    fun openFile(file: String): String
    fun closeFile(file: String)
    fun isOpen(file: String): Boolean
    fun getText(): String
}