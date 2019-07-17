package org.jb.cce.uast.statements.expressions

import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.statements.StatementNode

abstract class ExpressionNode(offset: Int,
                              length: Int) : StatementNode(offset, length) {
    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitExpressionNode(this)
    }
}