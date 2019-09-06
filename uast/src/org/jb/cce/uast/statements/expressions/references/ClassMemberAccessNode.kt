package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.Completable
import org.jb.cce.uast.CompositeNode
import org.jb.cce.uast.UnifiedAstVisitor

abstract class ClassMemberAccessNode(name: String,
                            offset: Int,
                            length: Int,
                            val isStatic: Boolean = false) : ReferenceNode(name, offset, length), CompositeNode, Completable {
    override fun accept(visitor: UnifiedAstVisitor) {
        prefix?.accept(visitor)
        visitor.visitClassMemberAccessNode(this)
    }
}