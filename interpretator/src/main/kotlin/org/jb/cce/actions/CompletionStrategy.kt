package org.jb.cce.actions

data class CompletionStrategy(val prefix: CompletionPrefix,
                              val statement: CompletionStatement,
                              val context: CompletionContext)

sealed class CompletionPrefix {
    class NoPrefix : CompletionPrefix()
    class CapitalizePrefix : CompletionPrefix()
    class SimplePrefix(val n: Int) : CompletionPrefix()
}

enum class CompletionStatement {
    METHOD_CALLS,
    ARGUMENTS,
    VARIABLES,
    ALL
}

enum class CompletionType {
    BASIC,
    SMART
}

enum class CompletionContext {
    ALL,
    PREVIOUS
}