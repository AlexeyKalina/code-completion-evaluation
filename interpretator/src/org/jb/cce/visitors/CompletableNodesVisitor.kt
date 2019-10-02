package org.jb.cce.visitors

import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.uast.Completable
import org.jb.cce.uast.statements.expressions.VariableAccessNode
import org.jb.cce.uast.statements.expressions.references.FieldAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode

class CompletableNodesVisitor(override val text: String, strategy: CompletionStrategy, textStart: Int):
        CallCompletionsVisitor(text, strategy, textStart) {

    override fun visitMethodCallNode(node: MethodCallNode) {
        node.prefix?.accept(this)
        if (checkFilters(node)) visitCompletable(node)
        visitChildren(node)
    }

    override fun visitVariableAccessNode(node: VariableAccessNode) {
        if (checkFilters(node)) visitCompletable(node)
    }

    override fun visitFieldAccessNode(node: FieldAccessNode) {
        node.prefix?.accept(this)
        if (checkFilters(node)) visitCompletable(node)
        visitChildren(node)
    }

    private fun checkFilters(node: Completable): Boolean = strategy.filters.all { it.value.shouldEvaluate(node.getProperties()) }
}