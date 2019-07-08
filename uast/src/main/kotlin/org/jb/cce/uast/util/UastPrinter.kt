package org.jb.cce.uast.util

import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstRecursiveVisitor

class UastPrinter : UnifiedAstRecursiveVisitor() {
    private var level = 0
    private val builder = StringBuilder()
    override fun visit(node: UnifiedAstNode) {
        builder.append("  ".repeat(level))
        builder.appendln(node::class.java.simpleName)
        level += 1
        super.visit(node)
        level -= 1
    }

    fun getText(): String {
        return builder.toString()
    }
}