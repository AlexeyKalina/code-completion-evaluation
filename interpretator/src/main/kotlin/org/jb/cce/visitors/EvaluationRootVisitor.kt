package org.jb.cce.visitors

import org.jb.cce.uast.*

abstract class EvaluationRootVisitor : UnifiedAstRecursiveVisitor() {
    abstract fun getRoot(): TextFragmentNode?
}

class EvaluationRootByOffsetVisitor(private val offset: Int, private val path: String, private val text: String) : EvaluationRootVisitor() {
    private var evaluationRoot: UnifiedAstNode? = null

    override fun visit(node: UnifiedAstNode) {
        if (evaluationRoot != null) return
        if (node is EvaluationRoot && node.contains(offset)) evaluationRoot = node
        else super.visit(node)
    }

    override fun getRoot(): TextFragmentNode? {
        val root = evaluationRoot ?: return null
        val textFragment = TextFragmentNode(root.getOffset(), root.getLength(), path, text)
        textFragment.addChild(root)
        return textFragment
    }
}

class DefaultEvaluationRootVisitor : EvaluationRootVisitor() {
    private var evaluationRoot: TextFragmentNode? = null

    override fun visitTextFragmentNode(node: TextFragmentNode) {
        evaluationRoot = node
    }

    override fun getRoot() = evaluationRoot
}