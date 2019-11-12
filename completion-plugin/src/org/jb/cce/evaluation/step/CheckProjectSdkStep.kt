package org.jb.cce.evaluation.step

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import org.jb.cce.EvaluationWorkspace

class CheckProjectSdkStep(private val project: Project) : EvaluationStep {
    override val name: String = "Check Project SDK"
    override val description: String = "Checks that project SDK was configured properly"

    override fun start(workspace: EvaluationWorkspace): EvaluationWorkspace? {
        if (ProjectRootManager.getInstance(project).projectSdk == null) {
            println("Project SDK not found. Evaluation cannot be fair.")
            println("Configure project sdk and start again.")
            return null
        }

        return workspace
    }
}