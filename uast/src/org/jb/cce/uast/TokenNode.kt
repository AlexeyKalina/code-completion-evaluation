package org.jb.cce.uast

import org.jb.cce.uast.exceptions.UnifiedAstException

class TokenNode(private val text: String, offset: Int, length: Int) : UnifiedAstNode(offset, length), Completable {
    override fun getChildren(): List<UnifiedAstNode> = emptyList()
    override fun getText() = text
    override fun accept(visitor: UnifiedAstVisitor) = visitor.visitTokenNode(this)
    override fun getProperties(): NodeProperties = throw UnifiedAstException("Token Node doesn't have properties")
}