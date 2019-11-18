package org.jb.cce

import com.intellij.openapi.project.rootManager
import com.jetbrains.python.statistics.modules
import junit.framework.TestCase
import org.jb.cce.actions.CompletionType
import org.jb.cce.evaluation.BackgroundStepFactory
import org.jb.cce.evaluation.EvaluationProcess
import org.jb.cce.evaluation.EvaluationRootInfo
import org.jb.cce.filter.impl.*
import org.jb.cce.uast.Language
import org.jb.cce.uast.TypeProperty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.FileReader
import java.nio.file.Paths

class CustomEvaluationTests : EvaluationTests() {
    private lateinit var workspace: EvaluationWorkspace

    @Test
    fun `interpret actions with smart completion`() = doTest("smart-completion.txt") {
        completionType = CompletionType.SMART
        evaluationTitle = CompletionType.SMART.name
    }

    @Test
    fun `interpret actions with ML completion`() = doTest("ml-completion.txt") {
        completionType = CompletionType.ML
        evaluationTitle = CompletionType.ML.name
    }

    @Test
    fun `interpret actions with sessions filter`() = doTest("sessions-filter.txt", "Only methods") {
        mergeFilters(listOf(SessionsFilter(
                "Only methods",
                mapOf(Pair(TypeFilterConfiguration.id, TypeFilter(listOf(TypeProperty.METHOD_CALL))))))
        )
    }

    @Test
    fun `interpret actions on random locations with seed`() = doTest("random-locations.txt") {
        completeTokenProbability = 0.5
        completeTokenSeed = 0
    }

    @Test
    fun `interpret actions on random locations with zero probability`() = doTest("zero-sessions.txt") {
        completeTokenProbability = 0.0
    }

    @Test
    fun `interpret actions on random locations with one probability`() = doTest("default-config.txt") {
        completeTokenProbability = 1.0
    }

    private fun doTest(reportName: String, filterName: String = SessionsFilter.ACCEPT_ALL.name, init: Config.Builder.() -> Unit) {
        val config = Config.build(tempDir.toString(), Language.JAVA.displayName) {
            evaluationRoots = project.modules.flatMap { it.rootManager.sourceRoots.map { it.path } }.toMutableList()
            init()
        }

        val process = EvaluationProcess.build({
            shouldInterpretActions = true
            shouldGenerateReports = true
        }, BackgroundStepFactory(config, project, true, null, EvaluationRootInfo(true)))

        val resultWorkspace = process.start(workspace)

        TestCase.assertTrue(
                "Actions files were generated",
                resultWorkspace.actionsStorage.getActionFiles().isEmpty())
        TestCase.assertEquals(
                "Sessions files count don't match source files count",
                resultWorkspace.sessionsStorage.getSessionFiles().size,
                SOURCE_FILES_COUNT)
        TestCase.assertTrue(
                "Report wasn't generated",
                resultWorkspace.reports.isNotEmpty())
        TestCase.assertEquals(
                "Reports don't match sessions filters",
                resultWorkspace.reports.keys,
                (config.reports.sessionsFilters + listOf(SessionsFilter.ACCEPT_ALL)).map { it.name }.toSet())

        val reportPath = resultWorkspace.reports[filterName]
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
        super.setUp()
        val config = Config.build(tempDir.toString(), Language.JAVA.displayName) {
            evaluationRoots = project.modules.flatMap { it.rootManager.sourceRoots.map { it.path } }.toMutableList()
        }
        workspace = EvaluationWorkspace.create(config)

        val process = EvaluationProcess.build({
            shouldGenerateActions = true
        }, BackgroundStepFactory(config, project, true, null, EvaluationRootInfo(true)))

        process.start(workspace)
        Thread.sleep(1_000)
    }
}