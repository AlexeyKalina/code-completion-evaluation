package org.jb.cce.visitors

import org.jb.cce.uast.EvaluationRoot
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstRecursiveVisitor

class EvaluationRootVisitor(private val offset: Int) : UnifiedAstRecursiveVisitor() {
    private var evaluationRoot: UnifiedAstNode? = null

    override fun visit(node: UnifiedAstNode) {
        if (evaluationRoot != null) return
        if (node is EvaluationRoot && node.contains(offset)) evaluationRoot = node
        else super.visit(node)
    }

    fun getRoot() = evaluationRoot
}