package org.jb.cce.actions

import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import java.lang.IllegalStateException


internal fun List<String>.buildMultipleEvaluationsConfig(): Config {
    var configBuilder: Config.Builder? = null
    for (workspacePath in this) {
        val workspace = EvaluationWorkspace.open(workspacePath)
        val currentConfig = workspace.readConfig()
        if (configBuilder == null) configBuilder = Config.Builder(currentConfig.projectPath, currentConfig.language)
        for (filter in workspace.readConfig().reports.sessionsFilters) {
            if (configBuilder.sessionsFilters.all { it.name != filter.name })
                configBuilder.sessionsFilters.add(filter)
        }
    }
    if (configBuilder == null) throw IllegalStateException("Empty list of workspaces")
    configBuilder.evaluationTitle = "COMPARE_MULTIPLE"
    return configBuilder.build()
}