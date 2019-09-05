package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.declarations.VariableDeclarationNode
import org.jb.cce.uast.statements.expressions.ExpressionNode

class MethodCallNode(name: String,
                     offset: Int,
                     length: Int,
                     isStatic: Boolean = false) : ClassMemberAccessNode(name, offset, length, isStatic) {
    override fun getText() = name

    private val arguments = mutableListOf<ExpressionNode>()

    fun addArgument(argument: ExpressionNode) {
        arguments += argument
    }

    override fun addChild(node: UnifiedAstNode) {
        if (node is VariableDeclarationNode) return
        if (node !is ExpressionNode) throw UnifiedAstException("Unexpected child: $node for $this")

        if (this.getOffset() > node.getOffset()) this.prefix = node
        else this.addArgument(node)
    }

    override fun getChildren() = arguments

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitMethodCallNode(this)
    }
}