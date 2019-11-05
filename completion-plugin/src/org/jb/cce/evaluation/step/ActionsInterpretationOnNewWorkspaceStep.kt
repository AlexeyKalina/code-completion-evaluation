package org.jb.cce.evaluation.step

import com.intellij.openapi.project.Project
import org.jb.cce.evaluation.ActionsInterpretationHandler

class ActionsInterpretationOnNewWorkspaceStep(project: Project, isHeadless: Boolean): CreateWorkspaceStep(ActionsInterpretationHandler(project), project, isHeadless) {
    override val name: String = "Actions interpreting"

    override val description: String = "Interpretation of generated actions"
}