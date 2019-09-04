package org.jb.cce.uast.statements.expressions

import org.jb.cce.uast.UnifiedAstVisitor

abstract class NamedNode(val name: String,
                         offset: Int,
                         length: Int,
                         val isStatic: Boolean = false) : ExpressionNode(offset, length) {
    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitNamedNode(this)
    }
}