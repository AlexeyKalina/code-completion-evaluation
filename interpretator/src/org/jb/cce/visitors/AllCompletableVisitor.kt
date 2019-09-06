package org.jb.cce.visitors

import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.uast.statements.expressions.VariableAccessNode
import org.jb.cce.uast.statements.expressions.references.ClassMemberAccessNode

class AllCompletableVisitor(override val text: String, strategy: CompletionStrategy, private val onlyStatic: Boolean, textStart: Int):
        CallCompletionsVisitor(text, strategy, textStart) {

    override fun visitClassMemberAccessNode(node: ClassMemberAccessNode) {
        node.prefix?.accept(this)
        if (!onlyStatic || node.isStatic) visitCompletable(node)
        visitChildren(node)
    }

    override fun visitVariableAccessNode(node: VariableAccessNode) {
        if (!onlyStatic) visitCompletable(node)
    }
}