package org.jb.cce.evaluation

import junit.framework.TestCase
import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.uast.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Tests on actions generation without further steps")
class GenerateActionsTests : EvaluationTests() {
    override val outputName: String = "generate-actions"

    @Test
    fun `generate actions with default config`() = doTest {}

    private fun doTest(init: Config.Builder.() -> Unit) {
        val roots = mutableListOf("src")
        val config = Config.build(tempDir.toString(), Language.JAVA.displayName) {
            evaluationRoots = roots
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
                sourceFilesCount(roots))
        TestCase.assertTrue(
                "Sessions files were generated",
                workspace.sessionsStorage.getSessionFiles().isEmpty())
        TestCase.assertTrue(
                "Report was generated",
                workspace.getReports().isEmpty())
    }
}