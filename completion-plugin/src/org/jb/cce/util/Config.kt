package org.jb.cce.util

import org.jb.cce.actions.CompletionContext
import org.jb.cce.actions.CompletionPrefix
import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.actions.CompletionType
import org.jb.cce.filter.EvaluationFilter
import java.nio.file.Paths

data class Config internal constructor(
        val projectPath: String,
        val language: String,
        val actionsGeneration: ActionsGeneration,
        val actionsInterpretation: ActionsInterpretation,
        val reportGeneration: ReportGeneration) {
    companion object {
        fun build(projectPath: String, language: String, init: Builder.() -> Unit): Config {
            val builder = Builder(projectPath, language)
            builder.init()
            return builder.build()
        }
    }

    data class ActionsGeneration internal constructor(
            val evaluationRoots: List<String>,
            val strategy: CompletionStrategy,
            val outputDir: String,
            val interpretActions: Boolean)

    data class ActionsInterpretation internal constructor(
            val completionType: CompletionType,
            val saveLogs: Boolean,
            val trainTestSplit: Int)

    data class ReportGeneration internal constructor(
            val evaluationTitle: String)

    class Builder(private val projectPath: String, private val language: String) {
        var evaluationRoots = mutableListOf<String>()
        var outputDir: String = Paths.get(projectPath, "completion-evaluation").toAbsolutePath().toString()
        var interpretActions: Boolean = true
        var saveLogs = false
        var trainTestSplit: Int = 70
        var completionType: CompletionType = CompletionType.BASIC
        var evaluationTitle: String = completionType.name
        var prefixStrategy: CompletionPrefix = CompletionPrefix.NoPrefix
        var contextStrategy: CompletionContext = CompletionContext.ALL
        var allTokens: Boolean = false

        internal fun build(): Config = Config(
                projectPath,
                language,
                ActionsGeneration(
                        evaluationRoots,
                        CompletionStrategy(prefixStrategy, contextStrategy, allTokens, filters),
                        outputDir,
                        interpretActions
                ),
                ActionsInterpretation(
                        completionType,
                        saveLogs,
                        trainTestSplit
                ),
                ReportGeneration(evaluationTitle)
        )

        var filters: MutableMap<String, EvaluationFilter> = mutableMapOf()
    }
}
