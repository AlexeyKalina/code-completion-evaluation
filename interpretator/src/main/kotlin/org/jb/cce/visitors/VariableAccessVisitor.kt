package org.jb.cce.visitors

import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.uast.statements.expressions.references.VariableAccessNode

class VariableAccessVisitor(override val text: String, strategy: CompletionStrategy): CallCompletionsVisitor(text, strategy) {

    override fun visitVariableAccessNode(node: VariableAccessNode) {
        visitToComplete(node)
    }
}