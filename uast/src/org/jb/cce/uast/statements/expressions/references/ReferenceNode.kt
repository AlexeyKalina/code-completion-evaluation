package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.statements.expressions.ExpressionNode
import org.jb.cce.uast.statements.expressions.NamedNode

abstract class ReferenceNode(name: String,
                             offset: Int,
                             length: Int) : NamedNode(name, offset, length) {

    var prefix: ExpressionNode? = null

    override fun accept(visitor: UnifiedAstVisitor) {
        prefix?.accept(visitor)
       visitor.visitReferenceNode(this)
    }
}
