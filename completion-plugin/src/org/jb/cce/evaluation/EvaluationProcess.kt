package org.jb.cce.evaluation

import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.rd.util.measureTimeMillis
import org.jb.cce.EvaluationWorkspace

class EvaluationProcess private constructor(private val steps: List<EvaluationStep>) {
    companion object {
        fun build(init: Builder.() -> Unit, stepFactory: StepFactory): EvaluationProcess {
            val builder = Builder()
            builder.init()
            return builder.build(stepFactory)
        }
    }

    fun start(workspace: EvaluationWorkspace) = ApplicationManager.getApplication().executeOnPooledThread {
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
    }

    class Builder {
        var shouldGenerateActions: Boolean = false
        var shouldInterpretActions: Boolean = false
        var shouldGenerateReports: Boolean = false
        var highlightInIde: Boolean = false

        fun build(factory: StepFactory): EvaluationProcess {
            val steps = mutableListOf<EvaluationStep>()

            if (shouldGenerateActions) {
                steps.add(factory.generateActionsStep())
            }

            if (shouldInterpretActions) {
                steps.add(factory.interpretActionsStep(!shouldGenerateActions, highlightInIde))
            }

            if (shouldGenerateReports) {
                steps.add(factory.generateReportStep())
            }

            steps.add(factory.finishEvaluation())

            return EvaluationProcess(steps)
        }
    }
}