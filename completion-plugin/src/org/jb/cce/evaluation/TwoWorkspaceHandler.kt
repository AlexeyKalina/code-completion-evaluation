package org.jb.cce.evaluation

import org.jb.cce.EvaluationWorkspace
import org.jb.cce.util.Progress

interface TwoWorkspaceHandler {
    fun invoke(workspace1: EvaluationWorkspace, workspace2: EvaluationWorkspace, indicator: Progress)
}