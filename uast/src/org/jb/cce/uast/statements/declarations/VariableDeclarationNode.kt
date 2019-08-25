package org.jb.cce.uast.statements.declarations

import org.jb.cce.uast.CompositeNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.expressions.ExpressionNode

class VariableDeclarationNode(name: String,
                              offset: Int,
                              length: Int) : DeclarationNode(name, offset, length), CompositeNode {

    private var initExpressions = mutableListOf<ExpressionNode>()

    fun addInitExpression(expression: ExpressionNode) {
        initExpressions.add(expression)
    }

    override fun addChild(node: UnifiedAstNode) {
        if (node is ExpressionNode) addInitExpression(node)
        else throw UnifiedAstException("Unexpected child: $node for $this")

    }

    override fun getChildren() = initExpressions

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitVariableDeclarationNode(this)
    }
}