package org.jb.cce.evaluation

import com.intellij.openapi.project.rootManager
import com.jetbrains.python.statistics.modules
import junit.framework.TestCase
import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.SessionsFilter
import org.jb.cce.actions.CompletionType
import org.jb.cce.filter.impl.TypeFilter
import org.jb.cce.filter.impl.TypeFilterConfiguration
import org.jb.cce.uast.Language
import org.jb.cce.uast.TypeProperty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CustomEvaluationTests : EvaluationTests() {
    private lateinit var workspace: EvaluationWorkspace
    override val outputName: String = "custom"

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

    @Test
    fun `generate file error on interpretation fail`() {
        val roots = mutableListOf("src")
        val config = Config.build(tempDir.toString(), Language.JAVA.displayName) {
            evaluationRoots = roots
        }
        val factory = BackgroundStepFactory(config, project, true, null, EvaluationRootInfo(true))
        factory.completionInvoker = ExceptionThrowingCompletionInvoker(factory.completionInvoker)
        val process = EvaluationProcess.build({
            shouldInterpretActions = true
            shouldGenerateReports = true
        }, factory)
        val resultWorkspace = process.start(workspace)

        TestCase.assertTrue(
                "Sessions files were generated",
                resultWorkspace.sessionsStorage.getSessionFiles().isEmpty())
        TestCase.assertEquals(
                "Error files count don't match source files count",
                resultWorkspace.errorsStorage.getErrors().size,
                sourceFilesCount(roots))
        checkReports(resultWorkspace, config, "zero-sessions.txt")
    }

    private fun doTest(reportName: String, filterName: String = SessionsFilter.ACCEPT_ALL.name, init: Config.Builder.() -> Unit) {
        val roots = mutableListOf("src")
        val config = Config.build(tempDir.toString(), Language.JAVA.displayName) {
            evaluationRoots = roots
            init()
        }

        val factory = BackgroundStepFactory(config, project, true, null, EvaluationRootInfo(true))
        factory.completionInvoker = FirstSuggestionCompletionInvoker(factory.completionInvoker)
        val process = EvaluationProcess.build({
            shouldInterpretActions = true
            shouldGenerateReports = true
        }, factory)

        val resultWorkspace = process.start(workspace)

        TestCase.assertTrue(
                "Actions files were generated",
                resultWorkspace.actionsStorage.getActionFiles().isEmpty())
        TestCase.assertEquals(
                "Sessions files count don't match source files count",
                resultWorkspace.sessionsStorage.getSessionFiles().size,
                sourceFilesCount(roots))
        checkReports(resultWorkspace, config, reportName, filterName)
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
        waitAfterWorkspaceCreated()
    }
}