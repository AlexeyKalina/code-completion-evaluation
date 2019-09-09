package org.jb.cce.uast

import org.jb.cce.uast.statements.AssignmentNode
import org.jb.cce.uast.statements.StatementNode
import org.jb.cce.uast.statements.declarations.*
import org.jb.cce.uast.statements.declarations.blocks.*
import org.jb.cce.uast.statements.expressions.*
import org.jb.cce.uast.statements.expressions.references.*

interface UnifiedAstVisitor {

    fun visitChildren(node: UnifiedAstNode) = node.getChildren().sortedBy { it.getOffset() }.forEach { it.accept(this) }

    fun visit(node: UnifiedAstNode) {}

    fun visitStatementNode(node: StatementNode) = visit(node)

    fun visitDeclarationNode(node: DeclarationNode) = visitStatementNode(node)

    fun visitBlockNode(node: BlockNode) = visit(node)

    fun visitExpressionNode(node: ExpressionNode) = visitStatementNode(node)
    fun visitLambdaExpressionNode(node: LambdaExpressionNode) = visitExpressionNode(node)
    fun visitAnonymousClassNode(node: AnonymousClassNode) = visitExpressionNode(node)

    fun visitArrayAccessNode(node: ArrayAccessNode) = visitExpressionNode(node)
    fun visitNamedNode(node: NamedNode) = visitExpressionNode(node)

    fun visitReferenceNode(node: ReferenceNode) = visitNamedNode(node)
    fun visitVariableAccessNode(node: VariableAccessNode) = visitNamedNode(node)

    fun visitClassInitializerNode(node: ClassInitializerNode) = visitBlockNode(node)
    fun visitGlobalNode(node: GlobalNode) = visitBlockNode(node)

    fun visitMethodBodyNode(node: MethodBodyNode) = visitBlockNode(node)
    fun visitArrayDeclarationNode(node: ArrayDeclarationNode) = visitDeclarationNode(node)
    fun visitClassDeclarationNode(node: ClassDeclarationNode) = visitDeclarationNode(node)
    fun visitClassHeaderNode(node: ClassHeaderNode) = visitDeclarationNode(node)
    fun visitMethodDeclarationNode(node: MethodDeclarationNode) = visitDeclarationNode(node)
    fun visitMethodHeaderNode(node: MethodHeaderNode) = visitDeclarationNode(node)

    fun visitVariableDeclarationNode(node: VariableDeclarationNode) = visitDeclarationNode(node)
    fun visitTypeReferenceNode(node: TypeReferenceNode) = visitReferenceNode(node)

    fun visitClassMemberAccessNode(node: ClassMemberAccessNode) = visitReferenceNode(node)
    fun visitMethodCallNode(node: MethodCallNode) = visitClassMemberAccessNode(node)
    fun visitFieldAccessNode(node: FieldAccessNode) = visitClassMemberAccessNode(node)

    fun visitAssignmentNode(node: AssignmentNode) = visitStatementNode(node)

    fun visitTokenNode(node: TokenNode) = visit(node)

    fun visitFileNode(node: FileNode) = visitTextFragmentNode(node)
    fun visitTextFragmentNode(node: TextFragmentNode) = visit(node)
}