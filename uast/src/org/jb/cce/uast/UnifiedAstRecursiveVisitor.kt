package org.jb.cce.uast

open class UnifiedAstRecursiveVisitor : UnifiedAstVisitor {
    override fun visit(node: UnifiedAstNode) {
        // TODO: use iteration instead of recursion ?
        visitChildren(node)
    }
}