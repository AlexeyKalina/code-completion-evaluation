package org.jb.cce.uast.statements.expressions.references

import org.jb.cce.uast.Completable
import org.jb.cce.uast.UnifiedAstNode

class FieldAccessNode(name: String,
                      offset: Int,
                      length: Int) : ReferenceNode(name, offset, length), Completable {
    override fun getChildren(): List<UnifiedAstNode> = listOf()

    override fun getText() = name
}