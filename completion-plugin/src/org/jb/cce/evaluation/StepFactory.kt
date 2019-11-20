package org.jb.cce.evaluation

import org.jb.cce.evaluation.step.EvaluationStep

interface StepFactory {
    fun generateActionsStep(): EvaluationStep
    fun interpretActionsStep(): EvaluationStep
    fun interpretActionsOnNewWorkspaceStep(): EvaluationStep
    fun highlightTokensInIdeStep(): EvaluationStep
    fun generateReportStep(): EvaluationStep
    fun finishEvaluationStep(): EvaluationStep

    fun setupStatsCollectorStep(): EvaluationStep?
    fun setupSdkStep(): EvaluationStep?
    fun checkSdkConfiguredStep(): EvaluationStep
}