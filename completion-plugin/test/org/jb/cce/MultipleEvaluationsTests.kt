package org.jb.cce

import com.intellij.openapi.project.rootManager
import com.jetbrains.python.statistics.modules
import junit.framework.TestCase
import org.jb.cce.actions.CompletionType
import org.jb.cce.actions.buildMultipleEvaluationsConfig
import org.jb.cce.evaluation.BackgroundStepFactory
import org.jb.cce.evaluation.EvaluationProcess
import org.jb.cce.evaluation.EvaluationRootInfo
import org.jb.cce.filter.impl.TypeFilter
import org.jb.cce.filter.impl.TypeFilterConfiguration
import org.jb.cce.uast.Language
import org.jb.cce.uast.TypeProperty
import org.junit.jupiter.api.Test
import java.io.FileReader
import java.nio.file.Paths

class MultipleEvaluationsTests : EvaluationTests() {

    @Test
    fun `compare basic and smart completion`() {
        val workspaces = listOf(createWorkspace(CompletionType.BASIC), createWorkspace(CompletionType.SMART))
        val resultConfig = workspaces.buildMultipleEvaluationsConfig()
        doTest("compare-basic-smart.txt", resultConfig, workspaces.map { it.path().toString() })
    }

    @Test
    fun `compare basic and ML completion`() {
        val workspaces = listOf(createWorkspace(CompletionType.BASIC), createWorkspace(CompletionType.ML))
        val resultConfig = workspaces.buildMultipleEvaluationsConfig()
        doTest("compare-basic-ml.txt", resultConfig, workspaces.map { it.path().toString() })
    }

    @Test
    fun `compare basic and smart completion with session filter`() {
        val filter = SessionsFilter(
                "Only methods",
                mapOf(Pair(TypeFilterConfiguration.id, TypeFilter(listOf(TypeProperty.METHOD_CALL))))
        )
        val workspaces = listOf(createWorkspace(CompletionType.BASIC, filter), createWorkspace(CompletionType.ML))
        val resultConfig = workspaces.buildMultipleEvaluationsConfig()
        doTest("compare-basic-ml-only-methods.txt", resultConfig, workspaces.map { it.path().toString() }, "Only methods")
    }

    private fun createWorkspace(completion: CompletionType, filter: SessionsFilter? = null): EvaluationWorkspace {
        val config = Config.build(tempDir.toString(), Language.JAVA.displayName) {
            evaluationRoots = project.modules.flatMap { it.rootManager.sourceRoots.map { it.path } }.toMutableList()
            completionType = completion
            evaluationTitle = completion.name
            if (filter != null) mergeFilters(listOf(filter))
        }
        val workspace = EvaluationWorkspace.create(config)
        val process = EvaluationProcess.build({
            shouldGenerateActions = true
            shouldInterpretActions = true
        }, BackgroundStepFactory(config, project, true, null, EvaluationRootInfo(true)))
        process.start(workspace)
        Thread.sleep(1_000)
        return workspace
    }

    private fun doTest(reportName: String, config: Config, workspaces: List<String>, filterName: String = SessionsFilter.ACCEPT_ALL.name) {
        val workspace = EvaluationWorkspace.create(config)
        val process = EvaluationProcess.build({
            shouldGenerateReports = true
        }, BackgroundStepFactory(config, project, true, workspaces, EvaluationRootInfo(true)))

        val resultWorkspace = process.start(workspace)

        TestCase.assertTrue(
                "Actions files were generated",
                resultWorkspace.actionsStorage.getActionFiles().isEmpty())
        TestCase.assertTrue(
                "Sessions files were generated",
                resultWorkspace.sessionsStorage.getSessionFiles().isEmpty())
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
}