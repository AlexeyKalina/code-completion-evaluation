package org.jb.cce

import com.google.gson.JsonObject
import org.jb.cce.uast.CompletableNode
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.exceptions.UnifiedAstException
import org.jb.cce.uast.statements.AssignmentNode
import org.jb.cce.uast.statements.declarations.ClassDeclarationNode
import org.jb.cce.uast.statements.declarations.VariableDeclarationNode
import org.jb.cce.uast.statements.declarations.blocks.BlockNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import org.jb.cce.uast.statements.expressions.references.VariableAccessNode

class BabelFishJavaVisitor: BabelFishUnifiedVisitor() {

    override fun visitFileNode(json: JsonObject): FileNode {
        val file = FileNode(getOffset(json), getLength(json))
        visitChildren(json, file)
        return file
    }

    override fun visitChild(json: JsonObject, parentNode: UnifiedAstNode) {
        when {
            typeEquals(json, "java:TypeDeclaration") -> visitTypeDeclaration(json, parentNode)
            typeEquals(json, "java:MethodInvocation") -> visitMethodCall(json, parentNode)
            typeEquals(json, "java:ExpressionMethodReference") -> visitMethodCall(json, parentNode)
            typeEquals(json, "java:ClassInstanceCreation") -> visitInstanceCreation(json, parentNode)
            typeEquals(json, "java:VariableDeclarationFragment") -> visitVariableDeclaration(json, parentNode)
            (roleExists(json, "Expression") || roleExists(json, "Argument") || roleExists(json, "Assignment"))
                && (typeEquals(json, "uast:Identifier") ||
                    typeEquals(json, "uast:QualifiedIdentifier")) -> visitVariableAccess(json, parentNode)
            typeEquals(json, "java:FieldAccess") -> visitVariableAccess(json, parentNode)
            else -> super.visitChild(json, parentNode)
        }
    }

    private fun visitInstanceCreation(json: JsonObject, parentNode: UnifiedAstNode) {
        val typeObj = json["type"].asJsonObject
        if (typeEquals(typeObj, "java:SimpleType")) {
            visitMethodCall(typeObj, parentNode)
        }
    }

    override fun visitVariableDeclaration(json: JsonObject, parentNode: UnifiedAstNode) {
        val variableDeclaration = VariableDeclarationNode(visitIdentifier(json["name"].asJsonObject), getOffset(json), getLength(json))
        when (parentNode) {
            is ClassDeclarationNode -> parentNode.addMember(variableDeclaration)
            is BlockNode -> parentNode.addStatement(variableDeclaration)
        }
    }

    override fun visitVariableAccess(json: JsonObject, parentNode: UnifiedAstNode) {
        val variableAccessNodes = visitCompletableNodes(json) { name, offset, length -> VariableAccessNode(name, offset, length) }
        for (node in variableAccessNodes) {
            when (parentNode) {
                is BlockNode -> parentNode.addStatement(node)
                is AssignmentNode -> parentNode.setAssigned(node)
                is MethodCallNode -> parentNode.addArgument(node)
            }
        }
    }

    override fun visitMethodCall(json: JsonObject, parentNode: UnifiedAstNode) {
        val methodCallNodes = visitCompletableNodes(json) { name, offset, length -> MethodCallNode(name, offset, length) }
        for (node in methodCallNodes) {
            when (parentNode) {
                is BlockNode -> parentNode.addStatement(node)
                is AssignmentNode -> parentNode.setAssigned(node)
                is MethodCallNode -> parentNode.addArgument(node)
            }
        }
        val lastNode = methodCallNodes.last()

        visitChildren(json, lastNode)
    }

    override fun visitTypeDeclaration(json: JsonObject, parentNode: UnifiedAstNode) {
        val name = if (json.has("name")) visitIdentifier(json["name"].asJsonObject) else ""
        val classDeclaration = ClassDeclarationNode(name, getOffset(json), getLength(json))

        visitChildren(json, classDeclaration)

        when (parentNode) {
            is FileNode -> parentNode.addDeclaration(classDeclaration)
            is ClassDeclarationNode -> parentNode.addMember(classDeclaration)
        }
    }
}