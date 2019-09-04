package org.jb.cce.visitors

import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.uast.statements.declarations.DeclarationNode
import org.jb.cce.uast.statements.expressions.references.ArrayAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import org.jb.cce.uast.statements.expressions.VariableAccessNode
import org.jb.cce.uast.statements.expressions.references.FieldAccessNode

class MethodCallsVisitor(override val text: String, strategy: CompletionStrategy, textStart: Int): CallCompletionsVisitor(text, strategy, textStart) {

    private var insideMethodCall = false

    override fun visitMethodCallNode(node: MethodCallNode) {
        val prevValue = insideMethodCall
        insideMethodCall = true
        node.prefix?.accept(this)
        visitCompletable(node)
        visitChildren(node)
        insideMethodCall = prevValue
    }

    override fun visitDeclarationNode(node: DeclarationNode) {
        val prevValue = insideMethodCall
        insideMethodCall = false
        super.visitDeclarationNode(node)
        insideMethodCall = prevValue
    }

    override fun visitVariableAccessNode(node: VariableAccessNode) {
        val prevValue = insideMethodCall
        insideMethodCall = false
        super.visitVariableAccessNode(node)
        insideMethodCall = prevValue
    }

    override fun visitFieldAccessNode(node: FieldAccessNode) {
        val prevValue = insideMethodCall
        insideMethodCall = false
        node.prefix?.accept(this)
        super.visitFieldAccessNode(node)
        insideMethodCall = prevValue
    }

    override fun visitArrayAccessNode(node: ArrayAccessNode) {
        val prevValue = insideMethodCall
        insideMethodCall = false
        node.prefix?.accept(this)
        super.visitArrayAccessNode(node)
        insideMethodCall = prevValue
    }
}