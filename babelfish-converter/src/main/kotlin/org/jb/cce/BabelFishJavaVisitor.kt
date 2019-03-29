package org.jb.cce

import com.google.gson.JsonObject
import org.jb.cce.uast.CompletableNode
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
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
                    typeEquals(json, "uast:QualifiedIdentifier")) -> visitFieldAccess(json, parentNode)
            typeEquals(json, "java:FieldAccess") -> visitFieldAccess(json, parentNode)
            else -> super.visitChild(json, parentNode)
        }
    }

    private fun visitInstanceCreation(json: JsonObject, parentNode: UnifiedAstNode) {
        val typeObj = json["type"].asJsonObject
        if (typeEquals(typeObj, "java:SimpleType")) {
            val nameObj = typeObj["name"].asJsonObject
            val methodCall = MethodCallNode(
                    CompletableNode(visitIdentifier(nameObj), getOffset(nameObj), getLength(nameObj)),
                    getOffset(json), getLength(json))

            visitChildren(json, methodCall)

            when (parentNode) {
                is BlockNode -> parentNode.addStatement(methodCall)
                is MethodCallNode -> parentNode.addArgument(methodCall)
            }
        }
    }

    override fun visitVariableDeclaration(json: JsonObject, parentNode: UnifiedAstNode) {
        val variableDeclaration = VariableDeclarationNode(visitIdentifier(json["name"].asJsonObject), getOffset(json), getLength(json))
        when (parentNode) {
            is ClassDeclarationNode -> parentNode.addMember(variableDeclaration as VariableDeclarationNode)
            is BlockNode -> parentNode.addStatement(variableDeclaration as VariableDeclarationNode)
        }
    }

    override fun visitFieldAccess(json: JsonObject, parentNode: UnifiedAstNode) {
        val name = when {
            typeEquals(json, "uast:QualifiedIdentifier") -> visitQualifiedIdentifier(json)
            json.has("name") -> CompletableNode(visitIdentifier(json["name"].asJsonObject),
                    getOffset(json["name"].asJsonObject), getLength(json["name"].asJsonObject))
            else -> CompletableNode(visitIdentifier(json), getOffset(json), getLength(json))
        }

        val variableAccess = VariableAccessNode(name, getOffset(json), getLength(json))

        when (parentNode) {
            is BlockNode -> parentNode.addStatement(variableAccess)
            is AssignmentNode -> parentNode.setAssigned(variableAccess)
            is MethodCallNode -> parentNode.addArgument(variableAccess)
        }
    }

    override fun visitMethodCall(json: JsonObject, parentNode: UnifiedAstNode) {
        val nameObj = json["name"].asJsonObject
        val methodCall = MethodCallNode(
                CompletableNode(visitIdentifier(nameObj), getOffset(nameObj), getLength(nameObj)),
                getOffset(json), getLength(json))

        visitChildren(json, methodCall)

        when (parentNode) {
            is BlockNode -> parentNode.addStatement(methodCall)
            is MethodCallNode -> parentNode.addArgument(methodCall)
        }
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