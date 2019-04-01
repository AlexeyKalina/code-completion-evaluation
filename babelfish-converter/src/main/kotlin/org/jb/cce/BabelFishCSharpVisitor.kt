package org.jb.cce

import com.google.gson.JsonObject
import org.jb.cce.uast.CompletableNode
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.statements.AssignmentNode
import org.jb.cce.uast.statements.declarations.ClassDeclarationNode
import org.jb.cce.uast.statements.declarations.blocks.BlockNode
import org.jb.cce.uast.statements.expressions.references.MethodCallNode
import org.jb.cce.uast.statements.expressions.references.VariableAccessNode

class BabelFishCSharpVisitor: BabelFishUnifiedVisitor() {
    override fun visitFileNode(json: JsonObject): FileNode {
        val file = FileNode(getOffset(json), getLength(json))
        visitChildren(json, file)
        return file
    }

    override fun visitChild(json: JsonObject, parentNode: UnifiedAstNode) {
        when {
            typeEquals(json, "csharp:ClassDeclaration") -> visitTypeDeclaration(json, parentNode)
            typeEquals(json, "csharp:InvocationExpression") -> visitMethodCall(json, parentNode)
            typeEquals(json, "csharp:ObjectCreationExpression") -> visitInstanceCreation(json, parentNode)
            typeEquals(json, "csharp:SimpleMemberAccessExpression") -> visitMemberAccess(json, parentNode)
            typeEquals(json, "csharp:Argument") -> visitArgument(json, parentNode)
            typeEquals(json, "csharp:SimpleAssignmentExpression") -> visitAssignment(json, parentNode)
            else -> super.visitChild(json, parentNode)
        }
    }

    private fun visitInstanceCreation(json: JsonObject, parentNode: UnifiedAstNode) {
        var typeObj = json["Type"].asJsonObject
        if (typeEquals(typeObj, "csharp:GenericName")) {
            typeObj = typeObj["Identifier"].asJsonObject
        }
        val methodCall = MethodCallNode(
                CompletableNode(visitIdentifier(typeObj), getOffset(typeObj), getLength(typeObj)),
                getOffset(typeObj), getLength(typeObj))

        visitChildren(json, methodCall)

        when (parentNode) {
            is BlockNode -> parentNode.addStatement(methodCall)
            is MethodCallNode -> parentNode.addArgument(methodCall)
        }
    }

    private fun visitAssignment(json: JsonObject, parentNode: UnifiedAstNode) {
        if (json.has("Left") && typeEquals(json["Left"].asJsonObject, "uast:Identifier")) {
            visitFieldAccess(json["Left"].asJsonObject, parentNode)
        }
        if (json.has("Right") && typeEquals(json["Right"].asJsonObject, "uast:Identifier")) {
            visitFieldAccess(json["Right"].asJsonObject, parentNode)
        }
        visitChildren(json, parentNode)
    }

    private fun visitArgument(json: JsonObject, parentNode: UnifiedAstNode) {
        if (json.has("Expression") && typeEquals(json["Expression"].asJsonObject, "uast:Identifier")) {
            visitFieldAccess(json["Expression"].asJsonObject, parentNode)
        } else {
            visitChildren(json, parentNode)
        }
    }

    private fun visitMemberAccess(json: JsonObject, parentNode: UnifiedAstNode) {
        visitChildren(json["Expression"].asJsonObject, parentNode)
        visitFieldAccess(json["Name"].asJsonObject, parentNode)
    }

    override fun visitFieldAccess(json: JsonObject, parentNode: UnifiedAstNode) {
        val nameObj = if (typeEquals(json, "csharp:GenericName")) json["Identifier"].asJsonObject else json
        val name = CompletableNode(visitIdentifier(nameObj), getOffset(nameObj), getLength(nameObj))
        val variableAccess = VariableAccessNode(name, getOffset(json), getLength(json))

        when (parentNode) {
            is BlockNode -> parentNode.addStatement(variableAccess)
            is AssignmentNode -> parentNode.setAssigned(variableAccess)
            is MethodCallNode -> parentNode.addArgument(variableAccess)
        }
    }

    override fun visitMethodCall(json: JsonObject, parentNode: UnifiedAstNode) {
        val exprObj = json["Expression"].asJsonObject
        var nameObj = when {
            exprObj["Name"].isJsonObject -> exprObj["Name"].asJsonObject
            else -> exprObj
        }
        if (typeEquals(nameObj, "csharp:GenericName")) {
            nameObj = nameObj["Identifier"].asJsonObject
        }

        val methodCall = MethodCallNode(
                CompletableNode(visitIdentifier(nameObj), getOffset(nameObj), getLength(nameObj)),
                getOffset(nameObj), getLength(nameObj))

        visitChildren(json, methodCall)

        when (parentNode) {
            is BlockNode -> parentNode.addStatement(methodCall)
            is MethodCallNode -> parentNode.addArgument(methodCall)
        }
    }

    override fun visitTypeDeclaration(json: JsonObject, parentNode: UnifiedAstNode) {
        val name = if (json.has("Identifier")) visitIdentifier(json["Identifier"].asJsonObject) else ""
        val classDeclaration = ClassDeclarationNode(name, getOffset(json), getLength(json))

        visitChildren(json, classDeclaration)

        when (parentNode) {
            is FileNode -> parentNode.addDeclaration(classDeclaration)
            is ClassDeclarationNode -> parentNode.addMember(classDeclaration)
        }
    }
}