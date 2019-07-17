package org.jb.cce.uast.statements

import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstVisitor

abstract class StatementNode(offset: Int,
                             length: Int) : UnifiedAstNode(offset, length) {
    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitStatementNode(this)
    }
}