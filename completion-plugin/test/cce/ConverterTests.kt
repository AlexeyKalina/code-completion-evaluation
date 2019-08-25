package org.jb.cce

import com.intellij.configurationStore.getOrCreateVirtualFile
import com.intellij.openapi.application.ReadAction
import com.intellij.testFramework.LightPlatformTestCase
import org.jb.cce.psi.PsiConverter
import org.jb.cce.uast.FileNode
import org.jb.cce.uast.Language
import org.jb.cce.uast.util.UastPrinter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.nio.file.Paths
import java.util.stream.Stream


class ConverterTests: LightPlatformTestCase() {
    companion object {
        private const val TEST_DATA_PATH = "testData"
        private const val OUTPUTS_NAME = "outs"
    }

    @ArgumentsSource(FileArgumentProvider::class)
    @ParameterizedTest(name = "{0}")
    fun doTest(testName: String, language: Language, testFile: File, testOutput: File) {
        println(testName)
        val virtualFile = getOrCreateVirtualFile(testFile.toPath(), null)
        val uast = ReadAction.compute<FileNode, Exception> { UastBuilder.create(getProject(), language).build(virtualFile) }
        val printer = UastPrinter()
        uast.accept(printer)
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
                org.junit.jupiter.api.fail("Unsupported languages found in the test data")
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
}