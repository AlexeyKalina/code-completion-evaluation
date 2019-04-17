package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.Completable
import org.jb.cce.uast.statements.StatementNode
import org.jb.cce.uast.statements.expressions.ExpressionNode

class MethodCallNode(name: String,
                     offset: Int,
                     length: Int) : ReferenceNode(name, offset, length), Completable {
    override fun getText() = name

    private val arguments = mutableListOf<StatementNode>()

    fun addArgument(argument: StatementNode) {
        arguments += argument
    }

    override fun getChildren() = arguments
}