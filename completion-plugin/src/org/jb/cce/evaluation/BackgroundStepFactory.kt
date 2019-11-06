package org.jb.cce.evaluation

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.io.exists
import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.HtmlReportGenerator
import org.jb.cce.dialog.OpenBrowserDialog
import org.jb.cce.evaluation.step.*
import java.nio.file.Paths
import kotlin.system.exitProcess

class BackgroundStepFactory(
        private val config: Config,
        private val project: Project,
        private val isHeadless: Boolean,
        private val inputWorkspacePaths: List<String>?,
        private val evaluationRootInfo: EvaluationRootInfo
) : StepFactory {

    override fun generateActionsStep(): EvaluationStep =
            ActionsGenerationStep(config.actions, config.language, evaluationRootInfo, project, isHeadless)

    override fun interpretActionsStep(): EvaluationStep =
            ActionsInterpretationStep(config.interpret, config.language, project, isHeadless)

    override fun generateReportStep(): EvaluationStep =
            ReportGenerationStep(inputWorkspacePaths?.map { EvaluationWorkspace(it, true) }, project, isHeadless)

    override fun interpretActionsOnNewWorkspaceStep(): EvaluationStep =
            ActionsInterpretationOnNewWorkspaceStep(config, project, isHeadless)

    override fun highlightTokensInIdeStep(): EvaluationStep =
            HighlightingTokensInIdeStep(project, isHeadless)

    override fun finishEvaluationStep(): EvaluationStep {
        return object : EvaluationStep {
            override val name: String = "Evaluation completed"
            override val description: String = "Correct termination of evaluation"

            override fun start(workspace: EvaluationWorkspace): EvaluationWorkspace? {
                val reportPath = Paths.get(workspace.reportsDirectory(), HtmlReportGenerator.pathToGlobalReport())
                when {
                    isHeadless -> {
                        println("Evaluation completed.${if (reportPath.exists()) " Report: $reportPath" else " Workspace: ${workspace.path()}"}")
                        exitProcess(0)
                    }
                    reportPath.exists() -> ApplicationManager.getApplication().invokeAndWait {
                        if (OpenBrowserDialog().showAndGet()) BrowserUtil.browse(reportPath.toString())
                    }
                    else -> ApplicationManager.getApplication().invokeAndWait{
                        Messages.showInfoMessage(project, "Evaluation completed", "Evaluation completed")
                    }
                }
                return workspace
            }
        }
    }
}