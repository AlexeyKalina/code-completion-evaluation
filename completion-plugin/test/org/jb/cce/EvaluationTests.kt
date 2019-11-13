package org.jb.cce

import com.intellij.debugger.impl.OutputChecker
import com.intellij.execution.ExecutionTestCase
import com.intellij.openapi.project.rootManager
import com.jetbrains.python.statistics.modules
import org.jb.cce.actions.CompletionContext
import org.jb.cce.actions.CompletionPrefix
import org.jb.cce.evaluation.BackgroundStepFactory
import org.jb.cce.evaluation.EvaluationProcess
import org.jb.cce.evaluation.EvaluationRootInfo
import org.jb.cce.uast.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
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
    fun `evaluate on default config`() = doTest {}

    @Test
    fun `evaluate with previous context`() = doTest {
        contextStrategy = CompletionContext.PREVIOUS
    }

    @Test
    fun `evaluate with emulating typing`() = doTest {
        prefixStrategy = CompletionPrefix.CapitalizePrefix(true)
    }

    @Test
    fun `evaluate with all tokens completion`() = doTest {
        allTokens = true
    }

    @Test
    fun `evaluate with all tokens completion and previous context`() = doTest {
        contextStrategy = CompletionContext.PREVIOUS
        allTokens = true
    }

    private fun doTest(init: Config.Builder.() -> Unit) {
        val config = Config.build(tempDir.toString(), Language.JAVA.displayName) {
            evaluationRoots = project.modules.flatMap { it.rootManager.sourceRoots.map { it.path } }.toMutableList()
            init()
        }
        val workspace = EvaluationWorkspace.create(config)

        val process = EvaluationProcess.build({
            shouldGenerateActions = true
            shouldInterpretActions = true
            shouldGenerateReports = true
            isTestingEnvironment = true
        }, BackgroundStepFactory(config, project, true, null, EvaluationRootInfo(true)))

        process.start(workspace)

        assert(workspace.actionsStorage.getActionFiles().size == SOURCE_FILES_COUNT) { "Actions files count doesn't match source files count" }
        assert(workspace.sessionsStorage.getSessionFiles().size == SOURCE_FILES_COUNT) { "Sessions files count doesn't match source files count" }
        assert(HtmlReportGenerator.resultReports(workspace.reportsDirectory()).isNotEmpty()) { "Report wasn't generated" }
    }

    @BeforeEach
    override fun setUp() {
        File(projectPath).copyRecursively(tempDir.toFile())
        super.setUp()
    }

    @AfterEach
    override fun tearDown() = super.tearDown()
}