package org.jb.cce
import com.google.gson.Gson
import org.jb.cce.actions.*
import org.junit.jupiter.api.Test
import java.io.File

class BabelFishConverterTests {

    private val endpoint = "0.0.0.0:9432"
    private val javaFilePath1 = "examples/files/Test.java"
    private val javaFilePath2 = "/home/kalina-alexey/projects/java/test/src/main/java/Test2.java"
    private val pythonFilePath = "examples/files/test.py"

    private fun parse(filePath: String, language: Language, resultPath: String) {
        val client = BabelFishClient("bblfsh_client", endpoint)
        val babelFishUast = client.parse(filePath)
        val uast = BabelFishConverter().convert(babelFishUast, language)
        File(resultPath).writeText(Gson().toJson(uast))
    }

    @Test
    fun parseJava() {
        parse(javaFilePath1, Language.JAVA, "examples/serialized/java.json")
    }

    @Test
    fun getActionsJava() {
        val client = BabelFishClient("bblfsh_client", endpoint)
        val babelFishUast = client.parse(javaFilePath1)
        val uast = BabelFishConverter().convert(babelFishUast, Language.JAVA)
        val strategy = CompletionStrategy(CompletionPrefix.CapitalizePrefix(), CompletionStatement.ALL, CompletionType.BASIC, CompletionContext.PREVIOUS)
        val actions = generateActions(javaFilePath1, File(javaFilePath1).readText(), uast, strategy)
        File("examples/actions/java.json").writeText(ActionSerializer().serialize(actions))
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