package org.jb.cce

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.jb.cce.uast.Language
import org.jb.cce.uast.TextFragmentNode
import org.jb.cce.uast.util.UastPrinter
import org.jb.cce.visitors.DefaultEvaluationRootVisitor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.nio.file.Paths
import java.util.stream.Stream

@DisplayName("Check UAST was built correctly")
class ConverterTests : BasePlatformTestCase() {
    companion object {
        private const val TEST_DATA_PATH = "testData"
        private const val OUTPUTS_NAME = "outs"
    }

    @ArgumentsSource(FileArgumentProvider::class)
    @ParameterizedTest(name = "{0}")
    fun doTest(testName: String, language: Language, testFile: File, testOutput: File) {
        Assumptions.assumeTrue(language != Language.ANOTHER)
        println(testName)
        val virtualFile = VfsUtil.findFileByIoFile(testFile, false) ?: kotlin.test.fail("virtual file not found")
        val uast = ReadAction.compute<TextFragmentNode, Exception> {
            UastBuilder.create(project, language.displayName, false).build(virtualFile, DefaultEvaluationRootVisitor())
        }
        val printer = UastPrinter()
        uast.accept(printer)
        val text = printer.getText()
        if (testOutput.exists()) {
            TestCase.assertEquals("Expected and actual uast structure mismatched", testOutput.readText(), text)
        } else {
            testOutput.parentFile.mkdirs()
            testOutput.writeText(text)
            fail("No expected output found. Do not forget to add the output into VCS")
        }
    }

    private class FileArgumentProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            val language2files = Paths.get(TEST_DATA_PATH).toFile().walkTopDown()
                    .onEnter { it.name != OUTPUTS_NAME }
                    .filter { it.isFile }
                .groupBy { Language.resolveByExtension(it.extension) }

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

    override fun getTestDataPath(): String {
        return TEST_DATA_PATH
    }

    @BeforeEach
    override fun setUp() {
        super.setUp()
        VfsRootAccess.allowRootAccess(testRootDisposable, Paths.get(TEST_DATA_PATH).toAbsolutePath().toString())
    }

    @AfterEach
    override fun tearDown() = super.tearDown()
}