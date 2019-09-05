package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstVisitor

class TypeReferenceNode(name: String,
                        offset: Int,
                        length: Int) : ReferenceNode(name, offset, length) {
    override fun getChildren() = emptyList<UnifiedAstNode>()

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitTypeReferenceNode(this)
    }
}