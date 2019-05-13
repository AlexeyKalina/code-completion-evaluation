package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.statements.expressions.NamedNode

abstract class ReferenceNode(name: String,
                             offset: Int,
                             length: Int) : NamedNode(name, offset, length) {

    var prefix: NamedNode? = null
}