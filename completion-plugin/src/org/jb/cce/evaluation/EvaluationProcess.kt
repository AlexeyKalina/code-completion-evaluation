package org.jb.cce.evaluation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.rd.util.measureTimeMillis
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.evaluation.step.EvaluationStep

class EvaluationProcess private constructor(private val steps: List<EvaluationStep>) {
    companion object {
        fun build(init: Builder.() -> Unit, stepFactory: StepFactory): EvaluationProcess {
            val builder = Builder()
            builder.init()
            return builder.build(stepFactory)
        }
    }

    fun startAsync(workspace: EvaluationWorkspace) = ApplicationManager.getApplication().executeOnPooledThread {
        start(workspace)
    }

    fun start(workspace: EvaluationWorkspace): EvaluationWorkspace {
        val stats = mutableMapOf<String, Long>()
        var currentWorkspace = workspace
        var hasError = false
        for (step in steps) {
            println("Starting step: ${step.name} (${step.description})")
            val duration = measureTimeMillis {
                val result = step.start(currentWorkspace)
                if (result == null) hasError = true
                else currentWorkspace = result
            }
            stats[step.name] = duration
            if (hasError) break
        }
        currentWorkspace.dumpStatistics(stats)
        return currentWorkspace
    }

    class Builder {
        var shouldGenerateActions: Boolean = false
        var shouldInterpretActions: Boolean = false
        var shouldGenerateReports: Boolean = false
        var shouldHighlightInIde: Boolean = false

        fun build(factory: StepFactory): EvaluationProcess {
            val steps = mutableListOf<EvaluationStep>()
            val isTestingEnvironment = ApplicationManager.getApplication().isUnitTestMode

            if (!isTestingEnvironment && (shouldGenerateActions || shouldInterpretActions)) {
                val setupSdkStep = factory.setupSdkStep()
                if (setupSdkStep != null) {
                    steps.add(setupSdkStep)
                }

                if (!Registry.`is`("evaluation.plugin.disable.sdk.check")) {
                    steps.add(factory.checkSdkConfiguredStep())
                }
            }

            if (shouldInterpretActions) {
                val setupStatsCollectorStep = factory.setupStatsCollectorStep()
                if (setupStatsCollectorStep != null) {
                    steps.add(setupStatsCollectorStep)
                }
            }

            if (shouldGenerateActions) {
                steps.add(factory.generateActionsStep())
            }

            if (shouldInterpretActions) {
                if (shouldGenerateActions) {
                    steps.add(factory.interpretActionsStep())
                } else {
                    steps.add(factory.interpretActionsOnNewWorkspaceStep())
                }
            }

            if (shouldHighlightInIde) {
                steps.add(factory.highlightTokensInIdeStep())
            }

            if (shouldGenerateReports) {
                steps.add(factory.generateReportStep())
            }

            if (!isTestingEnvironment) {
                steps.add(factory.finishEvaluationStep())
            }

            return EvaluationProcess(steps)
        }
    }
}