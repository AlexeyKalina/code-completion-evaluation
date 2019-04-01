package org.jb.cce.visitors

import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.uast.statements.declarations.DeclarationNode
import org.jb.cce.uast.statements.expressions.references.ArrayAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import org.jb.cce.uast.statements.expressions.references.VariableAccessNode

class MethodArgumentsVisitor(override val text: String, strategy: CompletionStrategy): CallCompletionsVisitor(text, strategy) {

    private var insideMethodCall = false;

    override fun visitMethodCallNode(node: MethodCallNode) {
        val prevValue = insideMethodCall
        insideMethodCall = true
        super.visitMethodCallNode(node)
        insideMethodCall = prevValue
    }

    override fun visitDeclarationNode(node: DeclarationNode) {
        val prevValue = insideMethodCall
        insideMethodCall = false
        super.visitDeclarationNode(node)
        insideMethodCall = prevValue
    }

    override fun visitVariableAccessNode(node: VariableAccessNode) {
        if (insideMethodCall) {
            visitToComplete(node.getVariableName())
        }
    }

    override fun visitArrayAccessNode(node: ArrayAccessNode) {
        val prevValue = insideMethodCall
        insideMethodCall = false
        super.visitArrayAccessNode(node)
        insideMethodCall = prevValue
    }
}