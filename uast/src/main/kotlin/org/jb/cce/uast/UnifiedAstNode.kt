package org.jb.cce.uast

abstract class UnifiedAstNode(private val offset: Int,
                              private val length: Int) {

    fun getOffset() = offset
    fun getLength() = length

    open fun accept(visitor: UnifiedAstVisitor) = visitor.visit(this)

    abstract fun getChildren(): List<UnifiedAstNode>
}