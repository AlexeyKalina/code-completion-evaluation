package org.jb.cce.uast.statements.declarations

import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstVisitor

class ClassHeaderNode(name: String,
                       offset: Int,
                       length: Int) : DeclarationNode(name, offset, length) {

    override fun getChildren() = emptyList<UnifiedAstNode>()

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitClassHeaderNode(this)
    }
}