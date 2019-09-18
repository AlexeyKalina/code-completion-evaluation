package org.jb.cce.visitors

import org.jb.cce.actions.ArgumentFilter
import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.actions.StaticFilter
import org.jb.cce.actions.TypeFilter
import org.jb.cce.uast.Completable
import org.jb.cce.uast.TypeProperty
import org.jb.cce.uast.statements.expressions.VariableAccessNode
import org.jb.cce.uast.statements.expressions.references.FieldAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode

class CompletableNodesVisitor(override val text: String, strategy: CompletionStrategy, textStart: Int):
        CallCompletionsVisitor(text, strategy, textStart) {

    override fun visitMethodCallNode(node: MethodCallNode) {
        node.prefix?.accept(this)
        if (checkFilters(node)) visitCompletable(node)
        visitChildren(node)
    }

    override fun visitVariableAccessNode(node: VariableAccessNode) {
        if (checkFilters(node)) visitCompletable(node)
    }

    override fun visitFieldAccessNode(node: FieldAccessNode) {
        node.prefix?.accept(this)
        if (checkFilters(node)) visitCompletable(node)
        visitChildren(node)
    }

    private fun checkFilters(node: Completable): Boolean {
        val properties = node.getProperties()
        val typeFilters = strategy.filters.typeFilters
        when (properties.typeProperty) {
            TypeProperty.METHOD_CALL -> if (!typeFilters.contains(TypeFilter.METHOD_CALLS)) return false
            TypeProperty.VARIABLE -> if (!typeFilters.contains(TypeFilter.VARIABLES)) return false
            TypeProperty.FIELD -> if (!typeFilters.contains(TypeFilter.FIELDS)) return false
        }
        val staticFilter = strategy.filters.staticFilter
        if (staticFilter != StaticFilter.ALL) {
            if (properties.isStatic && staticFilter == StaticFilter.NOT_STATIC ||
                    !properties.isStatic && staticFilter == StaticFilter.STATIC) return false

        }
        val argumentFilter = strategy.filters.argumentFilter
        if (argumentFilter != ArgumentFilter.ALL) {
            if (properties.isArgument && argumentFilter == ArgumentFilter.NOT_ARGUMENT ||
                    !properties.isArgument && argumentFilter == ArgumentFilter.ARGUMENT) return false
        }
        val packagePrefixFilter = strategy.filters.packagePrefixFilter
        if (!properties.packagePrefix.startsWith(packagePrefixFilter)) return false
        return true
    }
}