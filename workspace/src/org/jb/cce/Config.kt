package org.jb.cce

import org.jb.cce.actions.CompletionContext
import org.jb.cce.actions.CompletionPrefix
import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.actions.CompletionType
import org.jb.cce.filter.EvaluationFilter
import java.nio.file.Paths

data class Config internal constructor(
        val projectPath: String,
        val language: String,
        val outputDir: String,
        val interpretActions: Boolean,
        val actions: ActionsGeneration,
        val interpret: ActionsInterpretation,
        val reports: ReportGeneration) {
    companion object {
        fun build(projectPath: String, language: String, init: Builder.() -> Unit): Config {
            val builder = Builder(projectPath, language)
            builder.init()
            return builder.build()
        }
    }

    data class ActionsGeneration internal constructor(
            val evaluationRoots: List<String>,
            val strategy: CompletionStrategy)

    data class ActionsInterpretation internal constructor(
            val completionType: CompletionType,
            val completeTokenProbability: Double,
            val completeTokenSeed: Long?,
            val saveLogs: Boolean,
            val trainTestSplit: Int)

    data class ReportGeneration internal constructor(
            val evaluationTitle: String)

    class Builder internal constructor(private val projectPath: String, private val language: String) {
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
        var completeTokenProbability: Double = 1.0
        var completeTokenSeed: Long? = null

        internal fun build(): Config = Config(
                projectPath,
                language,
                outputDir,
                interpretActions,
                ActionsGeneration(
                        evaluationRoots,
                        CompletionStrategy(prefixStrategy, contextStrategy, allTokens, filters)
                ),
                ActionsInterpretation(
                        completionType,
                        completeTokenProbability,
                        completeTokenSeed,
                        saveLogs,
                        trainTestSplit
                ),
                ReportGeneration(evaluationTitle)
        )

        var filters: MutableMap<String, EvaluationFilter> = mutableMapOf()
    }
}
