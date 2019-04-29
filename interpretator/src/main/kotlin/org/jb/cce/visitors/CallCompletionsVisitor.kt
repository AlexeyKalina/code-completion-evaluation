package org.jb.cce.visitors

import org.jb.cce.TokenType
import org.jb.cce.actions.*
import org.jb.cce.exception.UnexpectedActionException
import org.jb.cce.uast.Completable
import org.jb.cce.uast.UnifiedAstVisitor
import org.jb.cce.uast.statements.declarations.blocks.BlockNode
import org.jb.cce.uast.statements.declarations.blocks.ClassInitializerNode
import org.jb.cce.uast.statements.declarations.blocks.GlobalNode
import org.jb.cce.uast.statements.declarations.blocks.MethodBodyNode
import org.jb.cce.uast.statements.expressions.VariableAccessNode
import org.jb.cce.uast.statements.expressions.references.FieldAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode

abstract class CallCompletionsVisitor(protected open val text: String,
                                      private val strategy: CompletionStrategy) : UnifiedAstVisitor {

    private val actions = mutableListOf<Action>()
    private val bracketSize = 1
    private var isInsideDeletedText = false
    private var previousTextStart = 0

    private val prefixCreator = when (strategy.prefix) {
        is CompletionPrefix.NoPrefix -> NoPrefixCreator()
        is CompletionPrefix.CapitalizePrefix -> CapitalizePrefixCreator()
        is CompletionPrefix.SimplePrefix -> SimplePrefixCreator(strategy.prefix.n)
    }

    fun getActions(): List<Action> = actions

    override fun visitClassInitializerNode(node: ClassInitializerNode) = visitDeletableBlock(node)

    override fun visitGlobalNode(node: GlobalNode) = visitDeletableBlock(node)

    override fun visitMethodBodyNode(node: MethodBodyNode) = visitDeletableBlock(node)

    private fun visitDeletableBlock(node: BlockNode) {

        if (strategy.context == CompletionContext.ALL) {
            visitChildren(node)
            return
        }

        if (isInsideDeletedText) return
        isInsideDeletedText = true
        previousTextStart = node.getOffset() + bracketSize
        visitChildren(node)
        isInsideDeletedText = false
        if (previousTextStart < node.getOffset() + node.getLength()) {
            actions += MoveCaret(previousTextStart)
            actions += PrintText(text.substring(IntRange(previousTextStart, node.getOffset() + node.getLength() - 1 - bracketSize)))
        }
    }

    protected fun visitCompletable(node: Completable) {
        when (strategy.context) {
            CompletionContext.ALL -> prepareAllContext(node)
            CompletionContext.PREVIOUS -> preparePreviousContext(node)
        }

        val tokenType = when (node) {
            is MethodCallNode -> TokenType.METHOD_CALL
            is VariableAccessNode -> TokenType.VARIABLE
            is FieldAccessNode -> TokenType.FIELD
            else -> throw UnexpectedActionException("Unknown token type")
        }

        val prefix = prefixCreator.getPrefix(node.getText())
        var currentPrefix = ""
        for (symbol in prefix) {
            actions += CallCompletion(currentPrefix, node.getText(), strategy.type, tokenType)
            actions += PrintText(symbol.toString())
            currentPrefix += symbol
        }
        actions += CallCompletion(prefix, node.getText(), strategy.type, tokenType)

        if (!prefix.isEmpty())
            actions += DeleteRange(node.getOffset(), node.getOffset() + prefix.length)
        actions += PrintText(node.getText())

        actions += CancelSession()
    }

    private fun prepareAllContext(node: Completable) {
        actions += DeleteRange(node.getOffset(), node.getOffset() + node.getLength())
        actions += MoveCaret(node.getOffset())
    }

    private fun preparePreviousContext(node: Completable) {
        if (previousTextStart < node.getOffset()) {
            actions += MoveCaret(previousTextStart)
            actions += PrintText(text.substring(IntRange(previousTextStart, node.getOffset() - 1)))
            previousTextStart = node.getOffset() + node.getLength()
            actions += MoveCaret(node.getOffset())
        }
    }
}