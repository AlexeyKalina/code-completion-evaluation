package org.jb.cce

import com.intellij.openapi.project.rootManager
import com.jetbrains.python.statistics.modules
import junit.framework.TestCase
import org.jb.cce.evaluation.BackgroundStepFactory
import org.jb.cce.evaluation.EvaluationProcess
import org.jb.cce.evaluation.EvaluationRootInfo
import org.jb.cce.uast.Language
import org.junit.jupiter.api.Test

class GenerateActionsTests : EvaluationTests() {
    @Test
    fun `generate actions with default config`() = doTest() {}

    private fun doTest(init: Config.Builder.() -> Unit) {
        val config = Config.build(tempDir.toString(), Language.JAVA.displayName) {
            evaluationRoots = project.modules.flatMap { it.rootManager.sourceRoots.map { it.path } }.toMutableList()
            init()
        }
        val workspace = EvaluationWorkspace.create(config)

        val process = EvaluationProcess.build({
            shouldGenerateActions = true
        }, BackgroundStepFactory(config, project, true, null, EvaluationRootInfo(true)))

        process.start(workspace)

        TestCase.assertEquals(
                "Actions files count don't match source files count",
                workspace.actionsStorage.getActionFiles().size,
                SOURCE_FILES_COUNT)
        TestCase.assertTrue(
                "Sessions files were generated",
                workspace.sessionsStorage.getSessionFiles().isEmpty())
        TestCase.assertTrue(
                "Report was generated",
                workspace.reports.isEmpty())
    }
}