package org.jb.cce.actions

data class CompletionStrategy(val prefix: CompletionPrefix,
                              val statement: CompletionStatement,
                              val context: CompletionContext)

sealed class CompletionPrefix(val emulateTyping: Boolean) {
    object NoPrefix : CompletionPrefix(false)
    class CapitalizePrefix(emulateTyping: Boolean) : CompletionPrefix(emulateTyping)
    class SimplePrefix(emulateTyping: Boolean, val n: Int) : CompletionPrefix(emulateTyping)
}

enum class CompletionStatement {
    METHOD_CALLS,
    ARGUMENTS,
    VARIABLES,
    ALL,
    ALL_STATIC,
    ALL_TOKENS
}

enum class CompletionType {
    BASIC,
    SMART,
    ML
}

enum class CompletionContext {
    ALL,
    PREVIOUS
}