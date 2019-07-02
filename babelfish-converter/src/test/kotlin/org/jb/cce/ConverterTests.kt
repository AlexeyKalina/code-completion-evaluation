package org.jb.cce

import org.jb.cce.uast.UnifiedAstNode
import org.jb.cce.uast.UnifiedAstRecursiveVisitor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.nio.file.Paths
import java.util.stream.Stream

class ConverterTests {
    companion object {
        private const val TEST_DATA_PATH = "testData"
        private const val OUTPUTS_NAME = "outs"
    }

    @ArgumentsSource(FileArgumentProvider::class)
    @ParameterizedTest(name = "{0}")
    fun doTest(testName: String, language: Language, testFile: File, testOutput: File) {
        println(testName)
        val client = BabelFishClient()
        val fileText = testFile.readText()
        val uast = client.parse(fileText, language)
        val convertedUast = BabelFishConverter().convert(uast, language)
        val printer = UastPrinter()
        convertedUast.accept(printer)
        val text = printer.getText()
        if (testOutput.exists()) {
            Assertions.assertEquals(text, testOutput.readText(), "Expected and actual uast structure mismatched")
        } else {
            testOutput.parentFile.mkdirs()
            testOutput.writeText(text)
            Assertions.fail("No expected output found. Do not forget to add the output into VCS")
        }
    }

    private class FileArgumentProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            val language2files = Paths.get(TEST_DATA_PATH).toFile().walkTopDown()
                    .onEnter { it.name != OUTPUTS_NAME }
                    .filter { it.isFile }
                    .groupBy { Language.resolve(it.extension) }

            val unsupportedFiles = language2files[Language.UNSUPPORTED]
            if (!unsupportedFiles.isNullOrEmpty()) {
                fail("Unsupported languages found in the test data")
            }

            fun asArguments(language: Language, testFiles: List<File>): Stream<Arguments> {
                return testFiles.stream().map {
                    val fileName = it.nameWithoutExtension
                    val outputsDirectory = File(it.parentFile, "outs")
                    val testName = "${language.displayName}:${it.nameWithoutExtension}"
                    Arguments.of(testName, language, it, File(outputsDirectory, "$fileName.out"))
                }
            }
            return language2files.entries.stream().flatMap { asArguments(it.key, it.value) }
        }
    }

    private class UastPrinter : UnifiedAstRecursiveVisitor() {
        private var level = 0
        private val builder = StringBuilder()
        override fun visit(node: UnifiedAstNode) {
            builder.append("  ".repeat(level))
            builder.appendln(node::class.java.simpleName)
            level += 1
            super.visit(node)
            level -= 1
        }

        fun getText(): String {
            return builder.toString()
        }
    }
}