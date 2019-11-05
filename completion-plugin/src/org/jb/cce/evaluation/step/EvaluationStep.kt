package org.jb.cce.evaluation.step

import org.jb.cce.EvaluationWorkspace

interface EvaluationStep {
    val name: String
    val description: String

    fun start(workspace: EvaluationWorkspace): EvaluationWorkspace?
}