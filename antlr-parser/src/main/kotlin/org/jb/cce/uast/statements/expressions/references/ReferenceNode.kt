package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.statements.expressions.ExpressionNode

abstract class ReferenceNode(@Transient private val name: String,
                             offset: Int,
                             length: Int) : ExpressionNode(offset, length) {

    protected var prefixReference: ReferenceNode? = null

    fun setPrefix(reference: ReferenceNode) {
        prefixReference = reference
    }

    fun getName() = name
}