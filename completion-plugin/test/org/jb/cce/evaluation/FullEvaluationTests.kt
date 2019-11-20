package org.jb.cce.evaluation

import com.intellij.openapi.project.rootManager
import com.jetbrains.python.statistics.modules
import junit.framework.TestCase
import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.SessionsFilter
import org.jb.cce.actions.CompletionContext
import org.jb.cce.actions.CompletionPrefix
import org.jb.cce.actions.CompletionType
import org.jb.cce.filter.impl.*
import org.jb.cce.uast.Language
import org.jb.cce.uast.TypeProperty
import org.junit.jupiter.api.Test

class FullEvaluationTests : EvaluationTests() {
    @Test
    fun `evaluate with default config`() = doTest("default-config.txt") {}

    @Test
    fun `evaluate with smart completion`() = doTest("smart-completion.txt") {
        completionType = CompletionType.SMART
        evaluationTitle = CompletionType.SMART.name
    }

    @Test
    fun `evaluate with ML completion`() = doTest("ml-completion.txt") {
        completionType = CompletionType.ML
        evaluationTitle = CompletionType.ML.name
    }

    @Test
    fun `evaluate with previous context`() = doTest("previous-context.txt") {
        contextStrategy = CompletionContext.PREVIOUS
    }

    @Test
    fun `evaluate with simple prefix`() = doTest("simple-prefix.txt") {
        prefixStrategy = CompletionPrefix.SimplePrefix(false, 2)
    }

    @Test
    fun `evaluate with capitalize prefix`() = doTest("capitalize-prefix.txt") {
        prefixStrategy = CompletionPrefix.CapitalizePrefix(false)
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
    fun `evaluate with token filters`() = doTest("token-filters.txt") {
        filters[TypeFilterConfiguration.id] = TypeFilter(listOf(TypeProperty.METHOD_CALL))
        filters[StaticFilterConfiguration.id] = StaticFilter(false)
        filters[ArgumentFilterConfiguration.id] = ArgumentFilter(false)
        filters[PackageRegexFilterConfiguration.id] = PackageRegexFilter(".*")
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

    @Test
    fun `evaluate on random locations with zero probability`() = doTest("zero-sessions.txt") {
        completeTokenProbability = 0.0
    }

    @Test
    fun `evaluate on random locations with one probability`() = doTest("default-config.txt") {
        completeTokenProbability = 1.0
    }

    private fun doTest(reportName: String, filterName: String = SessionsFilter.ACCEPT_ALL.name, init: Config.Builder.() -> Unit) {
        val config = Config.build(tempDir.toString(), Language.JAVA.displayName) {
            evaluationRoots = project.modules.flatMap { it.rootManager.sourceRoots.map { it.path } }.toMutableList()
            init()
        }
        val workspace = EvaluationWorkspace.create(config)

        val factory = BackgroundStepFactory(config, project, true, null, EvaluationRootInfo(true))
        factory.completionInvoker = FirstSuggestionCompletionInvoker(factory.completionInvoker)
        val process = EvaluationProcess.build({
            shouldGenerateActions = true
            shouldInterpretActions = true
            shouldGenerateReports = true
        }, factory)

        process.start(workspace)

        TestCase.assertEquals(
                "Actions files count don't match source files count",
                workspace.actionsStorage.getActionFiles().size,
                sourceFilesCount())
        TestCase.assertEquals(
                "Sessions files count don't match source files count",
                workspace.sessionsStorage.getSessionFiles().size,
                sourceFilesCount())
        checkReports(workspace, config, reportName, filterName)
    }
}