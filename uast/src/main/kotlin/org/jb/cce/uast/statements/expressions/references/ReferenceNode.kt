package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.statements.expressions.ExpressionNode
import org.jb.cce.uast.statements.expressions.NamedNode

abstract class ReferenceNode(name: String,
                             offset: Int,
                             length: Int) : NamedNode(name, offset, length) {

    var prefix: ReferenceNode? = null

//    fun setPrefix(reference: ReferenceNode) {
//        prefixReference = reference
//    }
}