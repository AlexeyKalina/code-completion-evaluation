package org.jb.cce.uast

import org.jb.cce.uast.statements.AssignmentNode
import org.jb.cce.uast.statements.StatementNode
import org.jb.cce.uast.statements.declarations.*
import org.jb.cce.uast.statements.declarations.blocks.*
import org.jb.cce.uast.statements.expressions.ExpressionNode
import org.jb.cce.uast.statements.expressions.NamedNode
import org.jb.cce.uast.statements.expressions.VariableAccessNode
import org.jb.cce.uast.statements.expressions.references.ArrayAccessNode
import org.jb.cce.uast.statements.expressions.references.FieldAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import org.jb.cce.uast.statements.expressions.references.ReferenceNode

interface UnifiedAstVisitor {

    fun visitChildren(node: UnifiedAstNode) = node.getChildren().sortedBy { it.getOffset() }.forEach { visit(it) }

    fun visit(node: UnifiedAstNode) {}

    fun visitStatementNode(node: StatementNode) = visit(node)

    fun visitDeclarationNode(node: DeclarationNode) = visitStatementNode(node)

    fun visitBlockNode(node: BlockNode) = visitDeclarationNode(node)

    fun visitExpressionNode(node: ExpressionNode) = visitStatementNode(node)

    fun visitNamedNode(node: NamedNode) = visitExpressionNode(node)

    fun visitReferenceNode(node: ReferenceNode) = visitNamedNode(node)

    fun visitClassInitializerNode(node: ClassInitializerNode) = visitBlockNode(node)
    fun visitGlobalNode(node: GlobalNode) = visitBlockNode(node)
    fun visitMethodBodyNode(node: MethodBodyNode) = visitBlockNode(node)
    fun visitNamedBlockNode(node: NamedBlockNode) = visitBlockNode(node)

    fun visitArrayDeclarationNode(node: ArrayDeclarationNode) = visitDeclarationNode(node)
    fun visitClassDeclarationNode(node: ClassDeclarationNode) = visitDeclarationNode(node)
    fun visitMethodDeclarationNode(node: MethodDeclarationNode) = visitDeclarationNode(node)
    fun visitMethodHeaderNode(node: MethodHeaderNode) = visitDeclarationNode(node)
    fun visitVariableDeclarationNode(node: VariableDeclarationNode) = visitDeclarationNode(node)

    fun visitArrayAccessNode(node: ArrayAccessNode) = visitReferenceNode(node)
    fun visitMethodCallNode(node: MethodCallNode) = visitReferenceNode(node)
    fun visitFieldAccessNode(node: FieldAccessNode) = visitReferenceNode(node)

    fun visitAssignmentNode(node: AssignmentNode) = visitStatementNode(node)
    fun visitVariableAccessNode(node: VariableAccessNode) = visitStatementNode(node)

    fun visitFileNode(node: FileNode) = visit(node)
}
