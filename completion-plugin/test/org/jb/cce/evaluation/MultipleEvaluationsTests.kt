package org.jb.cce.evaluation

import com.intellij.openapi.project.rootManager
import com.jetbrains.python.statistics.modules
import junit.framework.TestCase
import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.SessionsFilter
import org.jb.cce.actions.CompletionType
import org.jb.cce.actions.buildMultipleEvaluationsConfig
import org.jb.cce.filter.impl.TypeFilter
import org.jb.cce.filter.impl.TypeFilterConfiguration
import org.jb.cce.uast.Language
import org.jb.cce.uast.TypeProperty
import org.junit.jupiter.api.Test

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
        val factory = BackgroundStepFactory(config, project, true, null, EvaluationRootInfo(true))
        factory.completionInvoker = FirstSuggestionCompletionInvoker(factory.completionInvoker)
        val process = EvaluationProcess.build({
            shouldGenerateActions = true
            shouldInterpretActions = true
        }, factory)
        process.start(workspace)
        waitAfterWorkspaceCreated()
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
        checkReport(resultWorkspace, config, reportName, filterName)
    }
}