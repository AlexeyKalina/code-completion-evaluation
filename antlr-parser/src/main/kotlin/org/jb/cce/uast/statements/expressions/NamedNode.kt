package org.jb.cce.uast.statements.expressions

abstract class NamedNode(protected val name: String,
                         offset: Int,
                         length: Int) : ExpressionNode(offset, length)