package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.Completable
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.statements.expressions.ExpressionNode

class MethodCallNode(name: String,
                     offset: Int,
                     length: Int) : ReferenceNode(name, offset, length), Completable {
    override fun getText() = name

    private val arguments = mutableListOf<ExpressionNode>()

    fun addArgument(argument: ExpressionNode) {
        arguments += argument
    }

    override fun getChildren() = arguments

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitMethodCallNode(this)
    }
}