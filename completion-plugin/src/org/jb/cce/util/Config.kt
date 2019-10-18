package org.jb.cce.util

import org.jb.cce.actions.CompletionContext
import org.jb.cce.actions.CompletionPrefix
import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.actions.CompletionType
import org.jb.cce.filter.EvaluationFilter
import java.nio.file.Paths

data class Config constructor(
        val projectPath: String,
        val listOfFiles: List<String>,
        val language: String,
        val strategy: CompletionStrategy,
        val completionType: CompletionType,
        val workspaceDir: String,
        val interpretActions: Boolean,
        val saveLogs: Boolean,
        val trainTestSplit: Int) {
    companion object {
        fun build(projectPath: String, language: String, init: Builder.() -> Unit): Config {
            val builder = Builder(projectPath, language)
            builder.init()
            return builder.build()
        }
    }

    class Builder(private val projectPath: String, private val language: String) {
        var evaluationRoots = mutableListOf<String>()
        var workspaceDir: String = Paths.get(projectPath, "completion-evaluation").toAbsolutePath().toString()
        var interpretActions: Boolean = true
        var saveLogs = false
        var trainTestSplit: Int = 70
        var completionType: CompletionType = CompletionType.BASIC
        var prefixStrategy: CompletionPrefix = CompletionPrefix.NoPrefix
        var contextStrategy: CompletionContext = CompletionContext.ALL
        var allTokens: Boolean = false
        var filters: MutableMap<String, EvaluationFilter> = mutableMapOf()

        internal fun build(): Config = Config(
                projectPath,
                evaluationRoots,
                language,
                CompletionStrategy(prefixStrategy, contextStrategy, allTokens, filters),
                completionType,
                workspaceDir,
                interpretActions,
                saveLogs,
                trainTestSplit)
    }
}
