package org.jb.cce

import com.google.gson.JsonObject
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.statements.declarations.ClassDeclarationNode
import org.jb.cce.uast.statements.declarations.VariableDeclarationNode
import org.jb.cce.uast.statements.expressions.references.FieldAccessNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import org.jb.cce.uast.statements.expressions.VariableAccessNode

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
            typeEquals(json, "java:FieldAccess") -> visitFieldAccess(json, parentNode)
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
        addToParent(variableDeclaration, parentNode)
    }

    override fun visitFieldAccess(json: JsonObject, parentNode: UnifiedAstNode) {
        val fieldAccessNodes = visitNamedNodes(json) { name, offset, length -> FieldAccessNode(name, offset, length) }
        for (node in fieldAccessNodes) {
            addToParent(node, parentNode)
        }
    }

    override fun visitVariableAccess(json: JsonObject, parentNode: UnifiedAstNode) {
        val variableAccessNodes = visitNamedNodes(json) { name, offset, length -> VariableAccessNode(name, offset, length) }
        for (node in variableAccessNodes) {
            addToParent(node, parentNode)
        }
    }

    override fun visitMethodCall(json: JsonObject, parentNode: UnifiedAstNode) {
        val methodCallNodes = visitNamedNodes(json) { name, offset, length -> MethodCallNode(name, offset, length) }
        for (node in methodCallNodes) {
            addToParent(node, parentNode)
        }
        val lastNode = methodCallNodes.last()

        visitChildren(json, lastNode)
    }

    override fun visitTypeDeclaration(json: JsonObject, parentNode: UnifiedAstNode) {
        val name = if (json.has("name")) visitIdentifier(json["name"].asJsonObject) else ""
        val classDeclaration = ClassDeclarationNode(name, getOffset(json), getLength(json))

        visitChildren(json, classDeclaration)

        addToParent(classDeclaration, parentNode)
    }
}