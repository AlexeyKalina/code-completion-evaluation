package org.jb.cce.actions

data class CompletionStrategy(val prefix: CompletionPrefix,
                              val context: CompletionContext,
                              val completeAllTokens: Boolean,
                              val filters: Filters)

sealed class CompletionPrefix(val emulateTyping: Boolean) {
    object NoPrefix : CompletionPrefix(false)
    class CapitalizePrefix(emulateTyping: Boolean) : CompletionPrefix(emulateTyping)
    class SimplePrefix(emulateTyping: Boolean, val n: Int) : CompletionPrefix(emulateTyping)
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