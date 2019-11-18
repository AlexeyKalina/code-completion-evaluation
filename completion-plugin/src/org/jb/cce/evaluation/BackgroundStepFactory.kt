package org.jb.cce.evaluation

import com.intellij.openapi.project.Project
import org.jb.cce.CompletionInvoker
import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.evaluation.step.*
import org.jb.cce.interpretator.CompletionInvokerImpl
import org.jb.cce.interpretator.DelegationCompletionInvoker
import org.jb.cce.uast.Language

class BackgroundStepFactory(
        private val config: Config,
        private val project: Project,
        private val isHeadless: Boolean,
        private val inputWorkspacePaths: List<String>?,
        private val evaluationRootInfo: EvaluationRootInfo
) : StepFactory {

    var completionInvoker: CompletionInvoker = DelegationCompletionInvoker(CompletionInvokerImpl(project, config.interpret.completionType), project)

    override fun generateActionsStep(): EvaluationStep =
            ActionsGenerationStep(config.actions, config.language, evaluationRootInfo, project, isHeadless)

    override fun interpretActionsStep(): EvaluationStep =
            ActionsInterpretationStep(config.interpret, config.language, completionInvoker, project, isHeadless)

    override fun generateReportStep(): EvaluationStep =
            ReportGenerationStep(inputWorkspacePaths?.map { EvaluationWorkspace.open(it) }, config.reports.sessionsFilters, project, isHeadless)

    override fun interpretActionsOnNewWorkspaceStep(): EvaluationStep =
            ActionsInterpretationOnNewWorkspaceStep(config, completionInvoker, project, isHeadless)

    override fun highlightTokensInIdeStep(): EvaluationStep =
            HighlightingTokensInIdeStep(project, isHeadless)

    override fun setupSdkStep(): EvaluationStep? {
        return when (Language.resolve(config.language)) {
            Language.JAVA -> SetupJDKStep(project)
            else -> null
        }
    }

    override fun checkSdkConfiguredStep(): EvaluationStep = CheckProjectSdkStep(project)

    override fun finishEvaluationStep(): EvaluationStep =
            if (isHeadless) HeadlessFinishEvaluationStep() else UIFinishEvaluationStep(project)
}