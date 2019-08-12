package org.jb.cce.visitors

import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.uast.TokenNode

class AllTokensVisitor(text: String, strategy: CompletionStrategy) : CallCompletionsVisitor(text, strategy) {
    override fun visitTokenNode(node: TokenNode) = visitCompletable(node)
}