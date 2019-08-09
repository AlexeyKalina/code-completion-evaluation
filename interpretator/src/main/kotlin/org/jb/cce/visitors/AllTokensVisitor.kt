package org.jb.cce.visitors

import org.jb.cce.actions.CompletionContext
import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.uast.TokenNode

class AllTokensVisitor(text: String, strategy: CompletionStrategy) : CallCompletionsVisitor(text, strategy) {
    init {
//        if (strategy.context == CompletionContext.PREVIOUS)
//            throw IllegalArgumentException("Previous context is not allowed in All tokens completion")
    }

    override fun visitTokenNode(node: TokenNode) = visitCompletable(node)
}