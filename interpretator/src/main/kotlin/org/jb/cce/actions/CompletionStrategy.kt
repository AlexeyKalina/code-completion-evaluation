package org.jb.cce.actions

data class CompletionStrategy(val prefix: CompletionPrefix,
                              val statement: CompletionStatement,
                              val context: CompletionContext)

sealed class CompletionPrefix(val completePrevious: Boolean) {
    object NoPrefix : CompletionPrefix(false)
    class CapitalizePrefix(completePrevious: Boolean) : CompletionPrefix(completePrevious)
    class SimplePrefix(completePrevious: Boolean, val n: Int) : CompletionPrefix(completePrevious)
}

enum class CompletionStatement {
    METHOD_CALLS,
    ARGUMENTS,
    VARIABLES,
    ALL
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