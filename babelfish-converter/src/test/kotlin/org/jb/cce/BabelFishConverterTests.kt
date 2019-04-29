package org.jb.cce

import org.jb.cce.actions.*
import org.jb.cce.uast.FileNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.FileReader

class BabelFishConverterTests {
    private val javaFilePath1 = "examples/files/Test.java"
    private val javaFilePath2 = "examples/files/Test2.java"
    private val pythonFilePath = "examples/files/test.py"

    private fun parse(filePath: String, language: Language): FileNode {
        val client = BabelFishClient()
        val text = FileReader(filePath).use { it.readText() }
        val babelFishUast = client.parse(text, language)
        return BabelFishConverter().convert(babelFishUast, language)
    }

    private fun processActions(filePath: String, language: Language, strategy: CompletionStrategy) {
        val uast = parse(filePath, language)
        val text = FileReader(filePath).use { it.readText() }
        val actions = generateActions(filePath, text, uast, strategy)
        assertEquals(text, processActions(actions, text))
    }

    private fun processActions(actions: List<Action>, text: String): String {
        val sb = StringBuilder(text)
        var position = 0
        for (action in actions) {
            when (action) {
                is MoveCaret -> {
                    position = action.offset
                }
                is PrintText -> {
                    sb.insert(position, action.text)
                    position += action.text.length
                }
                is DeleteRange -> {
                    sb.delete(action.begin, action.end)
                    position = action.begin
                }
            }
        }
        return sb.toString()
    }

    @Test
    fun parseJava() {
        parse(javaFilePath1, Language.JAVA)
    }

    @Test
    fun parseJava2() {
        parse(javaFilePath2, Language.JAVA)
    }

    @Test
    fun parsePython() {
        parse(pythonFilePath, Language.PYTHON)
    }

    @Test
    fun interpretJavaActionsWithAllContext() {
        val strategy = CompletionStrategy(CompletionPrefix.CapitalizePrefix(), CompletionStatement.ALL, CompletionType.BASIC, CompletionContext.ALL)
        processActions(javaFilePath1, Language.JAVA, strategy)
    }

    @Test
    fun interpretJavaActionsWithPreviousContext() {
        val strategy = CompletionStrategy(CompletionPrefix.CapitalizePrefix(), CompletionStatement.ALL, CompletionType.BASIC, CompletionContext.PREVIOUS)
        processActions(javaFilePath1, Language.JAVA, strategy)
    }
}