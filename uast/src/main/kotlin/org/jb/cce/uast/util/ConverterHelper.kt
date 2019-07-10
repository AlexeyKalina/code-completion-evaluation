package org.jb.cce.uast.util

import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.AssignmentNode
import org.jb.cce.uast.statements.StatementNode
import org.jb.cce.uast.statements.declarations.ClassDeclarationNode
import org.jb.cce.uast.statements.declarations.DeclarationNode
import org.jb.cce.uast.statements.declarations.VariableDeclarationNode
import org.jb.cce.uast.statements.declarations.blocks.BlockNode
import org.jb.cce.uast.statements.expressions.ExpressionNode
import org.jb.cce.uast.statements.expressions.NamedNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import java.util.logging.Logger

object ConverterHelper {
    private val LOG = Logger.getLogger(this.javaClass.name)!!

    fun addToParent(node: UnifiedAstNode, parentNode: UnifiedAstNode) {
        when (parentNode) {
            is FileNode -> {
                when (node) {
                    is DeclarationNode -> parentNode.addDeclaration(node)
                    is StatementNode -> parentNode.addStatement(node)
                }
            }
            is ClassDeclarationNode -> {
                when (node) {
                    is DeclarationNode -> parentNode.addMember(node)
                    is StatementNode -> parentNode.addStatement(node)
                }
            }
            is VariableDeclarationNode -> parentNode.setInitExpression(node as ExpressionNode)
            is BlockNode -> parentNode.addStatement(node as StatementNode)
            is AssignmentNode -> parentNode.setAssigned(node as ExpressionNode)
            is MethodCallNode -> {
                if (node !is ExpressionNode) {
                    LOG.warning("$node is not Expression inside MethodCall")
                    return
                }
                if (parentNode.getOffset() > node.getOffset()) parentNode.prefix = node as NamedNode
                else parentNode.addArgument(node)
            }
            else -> throw UnifiedAstException("Unexpected parent $parentNode for node $node")
        }
    }
}