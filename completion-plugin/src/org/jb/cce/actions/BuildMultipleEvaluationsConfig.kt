package org.jb.cce.actions

import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace

fun List<EvaluationWorkspace>.buildMultipleEvaluationsConfig(): Config {
    val existingConfig = this.first().readConfig()
    return Config.build(existingConfig.projectPath, existingConfig.language) {
        for (workspace in this@buildMultipleEvaluationsConfig) {
            mergeFilters(workspace.readConfig().reports.sessionsFilters)
        }
        evaluationTitle = "COMPARE_MULTIPLE"
    }
}