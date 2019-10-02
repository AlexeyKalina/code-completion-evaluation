package org.jb.cce.uast.statements.expressions

import org.jb.cce.uast.Completable
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.NodeProperties

class VariableAccessNode(name: String,
                         offset: Int,
                         length: Int, private val properties: NodeProperties) : NamedNode(name, offset, length), Completable {
    override fun getChildren(): List<UnifiedAstNode> = listOf()

    override fun getText() = name

    override fun accept(visitor: UnifiedAstVisitor) {
        visitor.visitVariableAccessNode(this)
    }

    override fun getProperties() = properties
}