package org.jb.cce.evaluation.step

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.FutureResult
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.actions.ActionsGenerator
import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.evaluation.EvaluationRootInfo
import org.jb.cce.evaluation.UastBuilder
import org.jb.cce.exceptions.ExceptionsUtil.stackTraceToString
import org.jb.cce.info.FileErrorInfo
import org.jb.cce.util.*
import org.jb.cce.visitors.DefaultEvaluationRootVisitor
import org.jb.cce.visitors.EvaluationRootByOffsetVisitor
import org.jb.cce.visitors.EvaluationRootByRangeVisitor

class ActionsGenerationStep(private val evaluationRootInfo: EvaluationRootInfo, project: Project, isHeadless: Boolean): BackgroundEvaluationStep(project, isHeadless) {
    override val name: String = "Generating actions"

    override val description: String = "Generating actions by selected files"

    override fun start(workspace: EvaluationWorkspace): EvaluationWorkspace? {
        val result = FutureResult<EvaluationWorkspace?>()
        val task = object : Task.Backgroundable(project, name, true) {

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                val filesForEvaluation = FilesHelper.getFilesOfLanguage(project, workspace.config.actions.evaluationRoots, workspace.config.language)
                generateActions(workspace, workspace.config.language, filesForEvaluation, workspace.config.actions.strategy, evaluationRootInfo, getProgress(indicator))
            }

            override fun onSuccess() {
                result.set(workspace)
            }

            override fun onCancel() {
                evaluationAbortedHandler.onCancel(this.title)
                result.set(null)
            }

            override fun onThrowable(error: Throwable) {
                evaluationAbortedHandler.onError(error, this.title)
                result.set(null)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
        return result.get()
    }

    private fun generateActions(workspace: EvaluationWorkspace, languageName: String, files: Collection<VirtualFile>,
                                strategy: CompletionStrategy, evaluationRootInfo: EvaluationRootInfo, indicator: Progress) {
        val actionsGenerator = ActionsGenerator(strategy)
        val uastBuilder = UastBuilder.create(project, languageName, strategy.completeAllTokens)

        val errors = mutableListOf<FileErrorInfo>()
        for ((i, file) in files.withIndex()) {
            if (indicator.isCanceled()) {
                LOG.info("Generating actions is canceled by user. Done: $i/${files.size}. With error: ${errors.size}")
                break
            }
            LOG.info("Start generating actions for file ${file.path}. Done: $i/${files.size}. With error: ${errors.size}")
            val filename = file.name
            val progress = (i + 1).toDouble() / files.size
            var totalSessions = 0
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
                indicator.setProgress(filename, "${totalSessions.toString().padStart(3)} sessions | $filename", progress)
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