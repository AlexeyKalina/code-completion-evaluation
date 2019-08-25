package org.jb.cce.uast.statements.declarations.blocks

import org.jb.cce.uast.UnifiedAstVisitor

class MethodBodyNode(offset: Int,
                     length: Int) : BlockNode(offset, length) {
    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitMethodBodyNode(this)
    }
}