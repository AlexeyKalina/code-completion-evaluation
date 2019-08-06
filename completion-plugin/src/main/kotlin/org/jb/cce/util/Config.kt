package org.jb.cce.util

import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.actions.CompletionType
import org.jb.cce.uast.Language

data class Config(val projectPath: String,
                  val listOfFiles: List<String>,
                  val language: Language,
                  val strategy: CompletionStrategy,
                  val completionTypes: List<CompletionType>,
                  val outputDir: String,
                  val interpretActions: Boolean,
                  val saveLogs: Boolean,
                  val logsTrainingPercentage: Int)