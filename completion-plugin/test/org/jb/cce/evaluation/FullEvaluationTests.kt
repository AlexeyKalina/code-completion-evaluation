package org.jb.cce.evaluation

import junit.framework.TestCase
import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.SessionsFilter
import org.jb.cce.actions.CompletionContext
import org.jb.cce.actions.CompletionPrefix
import org.jb.cce.actions.CompletionType
import org.jb.cce.filter.EvaluationFilter
import org.jb.cce.filter.impl.*
import org.jb.cce.uast.Language
import org.jb.cce.uast.TypeProperty
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

@DisplayName("Tests on full evaluation process")
class FullEvaluationTests : EvaluationTests() {
    override val outputName: String = "full"

    @Test
    fun `evaluate with default config`() = doTest("default-config.txt") {}

    @Test
    fun `evaluate with root in package1`() = doTest("default-config-package1.txt",
            roots = mutableListOf("src${File.separator}package1")) {}

    @Test
    fun `evaluate with root in package2`() = doTest("default-config-package2.txt",
            roots = mutableListOf("src${File.separator}package2")) {}

    @Test
    fun `evaluate with root in package1 and package2`() = doTest("default-config-package1-package2.txt",
            roots = mutableListOf("src${File.separator}package1", "src${File.separator}package2")) {}

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
    fun `evaluate on methods`() = doTest("token-filters-methods.txt") {
        filters[TypeFilterConfiguration.id] = TypeFilter(listOf(TypeProperty.METHOD_CALL))
        filters[StaticFilterConfiguration.id] = EvaluationFilter.ACCEPT_ALL
        filters[ArgumentFilterConfiguration.id] = EvaluationFilter.ACCEPT_ALL
        filters[PackageRegexFilterConfiguration.id] = EvaluationFilter.ACCEPT_ALL
    }

    @Test
    fun `evaluate on static members`() = doTest("token-filters-static.txt") {
        filters[TypeFilterConfiguration.id] = TypeFilter(listOf(TypeProperty.METHOD_CALL, TypeProperty.FIELD))
        filters[StaticFilterConfiguration.id] = StaticFilter(true)
        filters[ArgumentFilterConfiguration.id] = EvaluationFilter.ACCEPT_ALL
        filters[PackageRegexFilterConfiguration.id] = EvaluationFilter.ACCEPT_ALL
    }

    @Test
    fun `evaluate on variable arguments`() = doTest("token-filters-arguments.txt") {
        filters[TypeFilterConfiguration.id] = TypeFilter(listOf(TypeProperty.VARIABLE))
        filters[StaticFilterConfiguration.id] = EvaluationFilter.ACCEPT_ALL
        filters[ArgumentFilterConfiguration.id] = ArgumentFilter(true)
        filters[PackageRegexFilterConfiguration.id] = EvaluationFilter.ACCEPT_ALL
    }

    @Test
    fun `evaluate on specific package`() = doTest("token-filters-package.txt") {
        filters[TypeFilterConfiguration.id] = EvaluationFilter.ACCEPT_ALL
        filters[StaticFilterConfiguration.id] = EvaluationFilter.ACCEPT_ALL
        filters[ArgumentFilterConfiguration.id] = EvaluationFilter.ACCEPT_ALL
        filters[PackageRegexFilterConfiguration.id] = PackageRegexFilter("package[12]")
    }

    @Test
    fun `evaluate on specific package on another root`() = doTest("zero-sessions.txt",
            roots = mutableListOf("src${File.separator}package1")) {
        filters[PackageRegexFilterConfiguration.id] = PackageRegexFilter("package2")
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

    private fun doTest(
            reportName: String,
            filterName: String = SessionsFilter.ACCEPT_ALL.name,
            roots: MutableList<String> = mutableListOf("src"),
            init: Config.Builder.() -> Unit) {
        val config = Config.build(tempDir.toString(), Language.JAVA.displayName) {
            evaluationRoots = roots
            init()
        }
        val workspace = EvaluationWorkspace.create(config)

        val factory = BackgroundStepFactory(config, project, true, null, EvaluationRootInfo(true))
        factory.customizeInvoker { FirstSuggestionCompletionInvoker(it) }
        val process = EvaluationProcess.build({
            shouldGenerateActions = true
            shouldInterpretActions = true
            shouldGenerateReports = true
        }, factory)

        process.start(workspace)

        TestCase.assertEquals(
                "Actions files count don't match source files count",
                workspace.actionsStorage.getActionFiles().size,
                sourceFilesCount(roots))
        TestCase.assertEquals(
                "Sessions files count don't match source files count",
                workspace.sessionsStorage.getSessionFiles().size,
                sourceFilesCount(roots))
        checkReports(workspace, config, reportName, filterName)
    }
}