package org.jb.cce.evaluation

interface StepFactory {
    fun generateActionsStep(): EvaluationStep
    fun interpretActionsStep(createWorkspace: Boolean, highlightInIde: Boolean): EvaluationStep
    fun generateReportStep(): EvaluationStep
    fun finishEvaluationStep(): EvaluationStep
}