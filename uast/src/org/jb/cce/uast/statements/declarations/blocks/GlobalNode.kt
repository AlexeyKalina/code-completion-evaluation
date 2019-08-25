package org.jb.cce.uast.statements.declarations.blocks

import org.jb.cce.uast.UnifiedAstVisitor

class GlobalNode(offset: Int,
                  length: Int) : BlockNode(offset, length) {
    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitGlobalNode(this)
    }
}