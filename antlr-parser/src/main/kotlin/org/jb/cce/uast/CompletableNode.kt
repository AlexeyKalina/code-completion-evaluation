package org.jb.cce.uast

import org.jb.cce.uast.statements.expressions.ExpressionNode

open class CompletableNode(private val text: String,
                      offset: Int,
                      length: Int): ExpressionNode(offset, length) {

    fun getText() = text

    override fun getChildren() = listOf<UnifiedAstNode>()
}