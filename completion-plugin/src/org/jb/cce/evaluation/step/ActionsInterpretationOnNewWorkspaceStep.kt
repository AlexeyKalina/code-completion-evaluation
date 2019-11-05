package org.jb.cce.evaluation.step

import com.intellij.openapi.project.Project
import org.jb.cce.evaluation.ActionsInterpretationHandler
import org.jb.cce.util.Config

class ActionsInterpretationOnNewWorkspaceStep(
        config: Config.ActionsInterpretation,
        private val language: String,
        project: Project,
        isHeadless: Boolean): CreateWorkspaceStep(ActionsInterpretationHandler(config, language, project), project, isHeadless) {
    override val name: String = "Actions interpreting"

    override val description: String = "Interpretation of generated actions"
}