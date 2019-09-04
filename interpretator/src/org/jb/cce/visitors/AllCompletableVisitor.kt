package org.jb.cce.visitors

import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.uast.statements.expressions.VariableAccessNode
import org.jb.cce.uast.statements.expressions.references.FieldAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import org.jb.cce.uast.statements.expressions.references.ReferenceNode

class AllCompletableVisitor(override val text: String, strategy: CompletionStrategy, private val onlyStatic: Boolean, textStart: Int):
        CallCompletionsVisitor(text, strategy, textStart) {

    override fun visitReferenceNode(node: ReferenceNode) {
        node.prefix?.accept(this)
        if (!onlyStatic || node.isStatic) visitCompletable(node)
        visitChildren(node)
    }

    override fun visitVariableAccessNode(node: VariableAccessNode) {
        if (!onlyStatic || node.isStatic) visitCompletable(node)
    }
}