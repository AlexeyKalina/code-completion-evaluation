package org.jb.cce.actions

abstract class CompletionPrefixCreator {
    abstract fun getPrefix(text: String): String
}

class NoPrefixCreator: CompletionPrefixCreator() {
    override fun getPrefix(text: String): String {
        return ""
    }
}

class SimplePrefixCreator(private val n: Int): CompletionPrefixCreator() {
    override fun getPrefix(text: String): String {
        return text.substring(0, n)
    }
}

class CapitalizePrefixCreator: CompletionPrefixCreator() {
    override fun getPrefix(text: String): String {
        return text.filterIndexed { i, ch -> i == 0 || ch.isUpperCase() }
    }
}