package org.jb.cce.uast.statements.expressions

import org.jb.cce.uast.UnifiedAstVisitor

abstract class NamedNode(protected val name: String,
                         offset: Int,
                         length: Int) : ExpressionNode(offset, length) {
    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitNamedNode(this)
    }
}