package org.jb.cce

import com.google.gson.Gson
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileReader

class BabelFishConverterTests {
    private val javaFilePath1 = "examples/files/Test.java"
    private val javaFilePath2 = "examples/files/Test2.java"
    private val pythonFilePath = "examples/files/test.py"

    private fun parse(filePath: String, language: Language, resultPath: String) {
        val client = BabelFishClient()
        val text = FileReader(filePath).use { it.readText() }
        val babelFishUast = client.parse(text, language)
        val uast = BabelFishConverter().convert(babelFishUast, language)
        File(resultPath).writeText(Gson().toJson(uast))
    }

    @Test
    fun parseJava() {
        parse(javaFilePath1, Language.JAVA, "examples/serialized/java.json")
    }

    @Test
    fun parseJava2() {
        parse(javaFilePath2, Language.JAVA, "examples/serialized/java2.json")
    }

    @Test
    fun parsePython() {
        parse(pythonFilePath, Language.PYTHON, "examples/serialized/python.json")
    }
}