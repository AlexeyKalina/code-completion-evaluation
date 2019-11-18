package org.jb.cce.evaluation.step

import com.intellij.openapi.project.Project
import org.jb.cce.CompletionInvoker
import org.jb.cce.Config
import org.jb.cce.evaluation.ActionsInterpretationHandler

class ActionsInterpretationOnNewWorkspaceStep(config: Config, completionInvoker: CompletionInvoker, project: Project, isHeadless: Boolean) :
            CreateWorkspaceStep(
                    config,
                    ActionsInterpretationHandler(config.interpret, config.language, completionInvoker, project),
                    project,
                    isHeadless) {

    override val name: String = "Actions interpreting"

    override val description: String = "Interpretation of generated actions"
}