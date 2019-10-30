package org.jb.cce.evaluation

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jb.cce.EvaluationWorkspace
import org.jb.cce.actions.ActionsGenerator
import org.jb.cce.actions.CompletionStrategy
import org.jb.cce.exceptions.ExceptionsUtil.stackTraceToString
import org.jb.cce.info.FileErrorInfo
import org.jb.cce.util.*
import org.jb.cce.visitors.DefaultEvaluationRootVisitor
import org.jb.cce.visitors.EvaluationRootByOffsetVisitor
import org.jb.cce.visitors.EvaluationRootByRangeVisitor

class ActionsGenerationEvaluator(project: Project, isHeadless: Boolean): BaseEvaluator(project, isHeadless) {

    fun evaluateUnderProgress(config: Config, offset: Int?, psi: PsiElement?) {
        val task = object : Task.Backgroundable(project, "Generating actions", true) {
            private val workspace = EvaluationWorkspace(config.outputDir)

            override fun run(indicator: ProgressIndicator) {
                indicator.text = this.title
                ConfigFactory.save(config, workspace.path())
                val filesForEvaluation = FilesHelper.getFilesOfLanguage(project, config.evaluationRoots, config.language)
                generateActions(workspace, config.language, filesForEvaluation, config.strategy, offset, psi, getProcess(indicator))
            }

            override fun onSuccess() {
                if (config.interpretActions) {
                    val interpretationEvaluator = ActionsInterpretationEvaluator(project, isHeadless)
                    interpretationEvaluator.evaluateUnderProgress(workspace, config, createWorkspace = false, generateReport = offset == null, highlight = offset != null)
                } else finisher.onSuccess()
            }

            override fun onCancel() = finisher.onCancel(this.title)

            override fun onThrowable(error: Throwable) = finisher.onError(error, this.title)
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun generateActions(workspace: EvaluationWorkspace, languageName: String, files: Collection<VirtualFile>,
                                strategy: CompletionStrategy, offset: Int?, psi: PsiElement?, indicator: Progress) {
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
                    psi != null -> EvaluationRootByRangeVisitor(psi.textRange?.startOffset ?: psi.textOffset,
                            psi.textRange?.endOffset ?:psi.textOffset + psi.textLength)
                    offset != null -> EvaluationRootByOffsetVisitor(offset, file.path, file.text())
                    else -> DefaultEvaluationRootVisitor()
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