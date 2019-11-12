package org.jb.cce.evaluation.step

abstract class FinishEvaluationStep : EvaluationStep {
    override val name: String = "Evaluation completed"
    override val description: String = "Correct termination of evaluation"
}