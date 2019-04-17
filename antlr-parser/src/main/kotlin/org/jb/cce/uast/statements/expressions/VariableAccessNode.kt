package org.jb.cce.uast.statements.expressions

import org.jb.cce.uast.Completable
import org.jb.cce.uast.UnifiedAstNode

class VariableAccessNode(private val name: String,
                         offset: Int,
                         length: Int) : ExpressionNode(offset, length), Completable {
    override fun getChildren(): List<UnifiedAstNode> = listOf()

    override fun getText() = name
}