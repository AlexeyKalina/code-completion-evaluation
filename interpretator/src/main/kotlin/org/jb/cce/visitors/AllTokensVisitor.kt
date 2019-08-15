package org.jb.cce.visitors

import org.jb.cce.actions.CompletionContext
import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.actions.MoveCaret
import org.jb.cce.actions.PrintText
import org.jb.cce.uast.TextFragmentNode
import org.jb.cce.uast.TokenNode

class AllTokensVisitor(text: String, strategy: CompletionStrategy, textStart: Int) : CallCompletionsVisitor(text, strategy, textStart) {
    override fun visitTokenNode(node: TokenNode) = visitCompletable(node)

    override fun visitTextFragmentNode(node: TextFragmentNode) {
        if (strategy.context == CompletionContext.ALL) {
            visitChildren(node)
            return
        }
        previousTextStart = node.getOffset()
        visitChildren(node)
        if (previousTextStart < node.getOffset() + node.getLength()) {
            actions += MoveCaret(previousTextStart)
            actions += PrintText(text.substring(IntRange(previousTextStart, node.getOffset() + node.getLength() - 1)))
        }
    }
}