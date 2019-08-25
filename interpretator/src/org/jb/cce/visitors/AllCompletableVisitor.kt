package org.jb.cce.visitors

import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.uast.statements.expressions.VariableAccessNode
import org.jb.cce.uast.statements.expressions.references.FieldAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode

class AllCompletableVisitor(override val text: String, strategy: CompletionStrategy, textStart: Int): CallCompletionsVisitor(text, strategy, textStart) {

    override fun visitMethodCallNode(node: MethodCallNode) {
        if (node.prefix != null) visit(node.prefix!!)
        visitCompletable(node)
        visitChildren(node)
    }
    override fun visitFieldAccessNode(node: FieldAccessNode) {
        if (node.prefix != null) visit(node.prefix!!)
        visitCompletable(node)
        visitChildren(node)
    }

    override fun visitVariableAccessNode(node: VariableAccessNode) = visitCompletable(node)
}