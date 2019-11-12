package org.jb.cce.evaluation.step

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.Config
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.actions.ActionsGenerator
import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.evaluation.EvaluationRootInfo
import org.jb.cce.evaluation.UastBuilder
import org.jb.cce.exceptions.ExceptionsUtil.stackTraceToString
import org.jb.cce.info.FileErrorInfo
import org.jb.cce.util.FilesHelper
import org.jb.cce.util.Progress
import org.jb.cce.util.text
import org.jb.cce.visitors.DefaultEvaluationRootVisitor
import org.jb.cce.visitors.EvaluationRootByOffsetVisitor
import org.jb.cce.visitors.EvaluationRootByRangeVisitor

class ActionsGenerationStep(
        private val config: Config.ActionsGeneration,
        private val language: String,
        private val evaluationRootInfo: EvaluationRootInfo,
        project: Project,
        isHeadless: Boolean): BackgroundEvaluationStep(project, isHeadless) {
    override val name: String = "Generating actions"

    override val description: String = "Generating actions by selected files"

    override fun runInBackground(workspace: EvaluationWorkspace, progress: Progress): EvaluationWorkspace {
        val filesForEvaluation = FilesHelper.getFilesOfLanguage(project, config.evaluationRoots, language)
        generateActions(workspace, language, filesForEvaluation, config.strategy, evaluationRootInfo, progress)
        return workspace
    }

    private fun generateActions(workspace: EvaluationWorkspace, languageName: String, files: Collection<VirtualFile>,
                                strategy: CompletionStrategy, evaluationRootInfo: EvaluationRootInfo, indicator: Progress) {
        val actionsGenerator = ActionsGenerator(strategy)
        val uastBuilder = UastBuilder.create(project, languageName, strategy.completeAllTokens)

        val errors = mutableListOf<FileErrorInfo>()
        var totalSessions = 0
        for ((i, file) in files.withIndex()) {
            if (indicator.isCanceled()) {
                LOG.info("Generating actions is canceled by user. Done: $i/${files.size}. With error: ${errors.size}")
                break
            }
            LOG.info("Start generating actions for file ${file.path}. Done: $i/${files.size}. With error: ${errors.size}")
            val filename = file.name
            val progress = (i + 1).toDouble() / files.size
            try {
                val rootVisitor = when {
                    evaluationRootInfo.useDefault -> DefaultEvaluationRootVisitor()
                    evaluationRootInfo.parentPsi != null -> EvaluationRootByRangeVisitor(
                            evaluationRootInfo.parentPsi.textRange?.startOffset ?: evaluationRootInfo.parentPsi.textOffset,
                            evaluationRootInfo.parentPsi.textRange?.endOffset ?: evaluationRootInfo.parentPsi.textOffset + evaluationRootInfo.parentPsi.textLength)
                    evaluationRootInfo.offset != null -> EvaluationRootByOffsetVisitor(evaluationRootInfo.offset, file.path, file.text())
                    else -> throw IllegalStateException("Parent psi and offset are null.")
                }
                val uast = uastBuilder.build(file, rootVisitor)
                val fileActions = actionsGenerator.generate(uast)
                workspace.actionsStorage.saveActions(fileActions)
                totalSessions += fileActions.sessionsCount
                indicator.setProgress(filename, "${totalSessions.toString().padStart(4)} sessions | $filename", progress)
            } catch (e: Throwable) {
                indicator.setProgress(filename, "error: ${e.message} | $filename", progress)
                workspace.errorsStorage.saveError(FileErrorInfo(FilesHelper.getRelativeToProjectPath(project, file.path), e.message
                        ?: "No Message", stackTraceToString(e)))
                LOG.error("Generating actions error for file ${file.path}.", e)
            }

            LOG.info("Generating actions for file ${file.path} completed. Done: $i/${files.size}. With error: ${errors.size}")
        }
    }
}