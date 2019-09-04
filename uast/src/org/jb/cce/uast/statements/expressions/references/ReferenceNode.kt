package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.Completable
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.statements.expressions.ExpressionNode
import org.jb.cce.uast.statements.expressions.NamedNode

abstract class ReferenceNode(name: String,
                             offset: Int,
                             length: Int,
                             isStatic: Boolean = false) : NamedNode(name, offset, length, isStatic), Completable {

    var prefix: ExpressionNode? = null

    override fun accept(visitor: UnifiedAstVisitor) {
        prefix?.accept(visitor)
       visitor.visitReferenceNode(this)
    }
}
