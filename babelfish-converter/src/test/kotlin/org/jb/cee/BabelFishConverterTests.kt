package org.jb.cee
import com.google.gson.Gson
import org.antlr.v4.runtime.BufferedTokenStream
import org.antlr.v4.runtime.CharStreams
import org.jb.cce.*
import org.junit.jupiter.api.Test
import java.io.File

class BabelFishConverterTests {

    private val clientLibPath = "examples/bblfsh_client.so"
    private val endpoint = "0.0.0.0:9432"
    private val javaFilePath1 = "examples/files/Test.java"
    private val javaFilePath2 = "examples/files/Test2.java"
    private val pythonFilePath = "examples/files/test.py"

    private fun parse(filePath: String, language: Language, resultPath: String) {
        val client = BabelFishClient(clientLibPath, endpoint)
        val babelFishUast = client.parse(filePath)
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

    @Test
    fun serializeUastJava() {
        val lexer = Java8Lexer(CharStreams.fromFileName(javaFilePath1))
        val parser = Java8Parser(BufferedTokenStream(lexer))
        val tree = JavaVisitor().buildUnifiedAst(parser)
        File("examples/serialized/ourJava.json").writeText(Gson().toJson(tree))
    }
}