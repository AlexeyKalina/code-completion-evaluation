package org.jb.cce.uast.statements.expressions

import org.jb.cce.uast.CompositeNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.declarations.VariableDeclarationNode
import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode

class LambdaExpressionNode(offset: Int, length: Int): ExpressionNode(offset, length), CompositeNode {
    private var parameters = mutableListOf<VariableDeclarationNode>()
    private var expression: ExpressionNode? = null
    private var bodyBlock: MethodBodyNode? = null

    fun addParameter(variable: VariableDeclarationNode) {
        parameters.add(variable)
    }

    fun setExpression(expression: ExpressionNode) {
        this.expression = expression
    }

    fun setBody(body: MethodBodyNode) {
        bodyBlock = body
    }

    override fun addChild(node: UnifiedAstNode) {
        if (node is ExpressionNode) setExpression(node)
        else throw UnifiedAstException("Unexpected child: $node for $this")
    }

    override fun getChildren(): List<UnifiedAstNode> = parameters + listOfNotNull(expression, bodyBlock)

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitLambdaExpressionNode(this)
    }
}