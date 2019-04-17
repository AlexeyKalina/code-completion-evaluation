package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.statements.expressions.ExpressionNode

abstract class ReferenceNode(protected val name: String,
                             offset: Int,
                             length: Int) : ExpressionNode(offset, length) {

    var prefix: ReferenceNode? = null

//    fun setPrefix(reference: ReferenceNode) {
//        prefixReference = reference
//    }
}