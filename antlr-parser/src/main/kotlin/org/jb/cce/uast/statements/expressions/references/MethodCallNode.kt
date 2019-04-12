package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.CompletableNode
import org.jb.cce.uast.statements.expressions.ExpressionNode

class MethodCallNode(name: String,
                     offset: Int,
                     length: Int) : CompletableNode(name, offset, length) {

    private val arguments = mutableListOf<ExpressionNode>()

    fun addArgument(argument: ExpressionNode) {
        arguments += argument
    }

    override fun getChildren() = arguments
}