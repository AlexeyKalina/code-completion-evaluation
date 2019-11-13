package org.jb.cce

import com.intellij.debugger.impl.OutputChecker
import com.intellij.execution.ExecutionTestCase
import com.intellij.openapi.project.rootManager
import com.jetbrains.python.statistics.modules
import junit.framework.TestCase
import org.jb.cce.actions.CompletionContext
import org.jb.cce.actions.CompletionPrefix
import org.jb.cce.actions.CompletionType
import org.jb.cce.evaluation.BackgroundStepFactory
import org.jb.cce.evaluation.EvaluationProcess
import org.jb.cce.evaluation.EvaluationRootInfo
import org.jb.cce.filter.impl.TypeFilter
import org.jb.cce.filter.impl.TypeFilterConfiguration
import org.jb.cce.uast.Language
import org.jb.cce.uast.TypeProperty
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileReader
import java.nio.file.Path
import java.nio.file.Paths


class EvaluationTests : ExecutionTestCase()  {
    companion object {
        private const val PROJECT_NAME = "test-project"
        private const val SOURCE_FILES_COUNT = 2
        private val projectPath = Paths.get(PROJECT_NAME).toAbsolutePath().toString()
    }

    @TempDir
    lateinit var tempDir: Path

    override fun initOutputChecker(): OutputChecker = OutputChecker(tempDir.toString(), tempDir.toString())

    override fun getTestAppPath(): String = tempDir.toString()

    override fun getName(): String = PROJECT_NAME

    @Test
    fun `evaluate with default config`() = doTest("default-config.txt") {}

    @Test
    fun `evaluate with smart completion`() = doTest("smart-completion.txt") {
        completionType = CompletionType.SMART
        evaluationTitle = CompletionType.SMART.name
    }

    @Test
    fun `evaluate with previous context`() = doTest("previous-context.txt") {
        contextStrategy = CompletionContext.PREVIOUS
    }

    @Test
    fun `evaluate with emulating typing`() = doTest("emulate-typing.txt") {
        prefixStrategy = CompletionPrefix.CapitalizePrefix(true)
    }

    @Test
    fun `evaluate with all tokens completion`() = doTest("all-tokens.txt") {
        allTokens = true
    }

    @Test
    fun `evaluate with sessions filter`() = doTest("sessions-filter.txt", "Only methods") {
        mergeFilters(listOf(SessionsFilter(
                "Only methods",
                mapOf(Pair(TypeFilterConfiguration.id, TypeFilter(listOf(TypeProperty.METHOD_CALL))))))
        )
    }

    @Test
    fun `evaluate on random locations with seed`() = doTest("random-locations.txt") {
        completeTokenProbability = 0.5
        completeTokenSeed = 0
    }

    private fun doTest(reportName: String, filterName: String = SessionsFilter.ACCEPT_ALL.name, init: Config.Builder.() -> Unit) {
        val config = Config.build(tempDir.toString(), Language.JAVA.displayName) {
            evaluationRoots = project.modules.flatMap { it.rootManager.sourceRoots.map { it.path } }.toMutableList()
            init()
        }
        val workspace = EvaluationWorkspace.create(config)

        val process = EvaluationProcess.build({
            shouldGenerateActions = true
            shouldInterpretActions = true
            shouldGenerateReports = true
        }, BackgroundStepFactory(config, project, true, null, EvaluationRootInfo(true)))

        process.start(workspace)

        TestCase.assertEquals(
                "Actions files count don't match source files count",
                workspace.actionsStorage.getActionFiles().size,
                SOURCE_FILES_COUNT)
        TestCase.assertEquals(
                "Sessions files count don't match source files count",
                workspace.sessionsStorage.getSessionFiles().size,
                SOURCE_FILES_COUNT)
        TestCase.assertTrue(
                "Report wasn't generated",
                workspace.reports.isNotEmpty())
        TestCase.assertEquals(
                "Reports don't match sessions filters",
                workspace.reports.keys,
                (config.reports.sessionsFilters + listOf(SessionsFilter.ACCEPT_ALL)).map { it.name }.toSet())

        val reportPath = workspace.reports[filterName]
        val reportText = FileReader(reportPath.toString()).use { it.readText() }
        val testOutput = Paths.get(projectPath, "out", reportName).toFile()
        if (testOutput.exists()) {
            val testOutputText = FileReader(testOutput).use { it.readText() }
            TestCase.assertEquals(
                    "Expected and actual reports mismatched",
                    reportText,
                    testOutputText)
        } else {
            testOutput.writeText(reportText)
            fail("No expected output found. Do not forget to add the output into VCS")
        }

    }

    @BeforeEach
    override fun setUp() {
        File(projectPath).copyRecursively(tempDir.toFile())
        super.setUp()
    }

    @AfterEach
    override fun tearDown() = super.tearDown()
}