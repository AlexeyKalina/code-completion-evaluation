package org.jb.cce.uast

open class TextFragmentNode(offset: Int,
                   length: Int, val path: String, val text: String) : UnifiedAstNode(offset, length), CompositeNode {
    private val children = mutableListOf<UnifiedAstNode>()

    override fun getChildren(): List<UnifiedAstNode> = children

    override fun addChild(node: UnifiedAstNode) {
        children.add(node)
    }

    override fun accept(visitor: UnifiedAstVisitor) = visitor.visitTextFragmentNode(this)
}