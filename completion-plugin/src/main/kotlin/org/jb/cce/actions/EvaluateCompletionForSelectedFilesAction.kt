package org.jb.cce.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import org.apache.commons.io.input.UnixLineEndingInputStream
import org.jb.cce.*
import org.jb.cce.interpretator.CompletionInvokerImpl
import org.jb.cce.interpretator.DelegationCompletionInvoker
import org.jb.cce.metrics.MetricsEvaluator
import org.jb.cce.uast.Language

class EvaluateCompletionForSelectedFilesAction : AnAction() {
    private companion object {
        val LOG = Logger.getInstance(EvaluateCompletionForSelectedFilesAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val language2files = getFiles(e)
        if (language2files.isEmpty()) {
            Messages.showInfoMessage(project, "Languages of selected files aren't supported.", "Nothing to complete")
            return
        }

        val settingsDialog = CompletionSettingsDialog(project, language2files)
        val result = settingsDialog.showAndGet()
        if (!result) return

        val strategy = CompletionStrategy(settingsDialog.completionPrefix, settingsDialog.completionStatement, settingsDialog.completionContext)
        val completionTypes = settingsDialog.completionTypes
        val task = object : Task.Backgroundable(project, "Generating actions for selected files", true) {
            private lateinit var actions: List<Action>
            override fun run(indicator: ProgressIndicator) {
                actions = generateActions(project, settingsDialog.language, language2files.getValue(settingsDialog.language), strategy, indicator)
            }

            override fun onSuccess() {
                interpretUnderProgress(actions, completionTypes, project, settingsDialog.outputDir)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun generateActions(project: Project, language: Language, files: Collection<VirtualFile>, strategy: CompletionStrategy, indicator: ProgressIndicator): List<Action> {
        val actionsGenerator = ActionsGenerator(strategy)
        val uastBuilder = UastBuilder.create(project, language)

        val sortedFiles = files.sortedBy { f -> f.name }
        val generatedActions = mutableListOf<List<Action>>()
        var completed = 0
        var withError = 0
        for (file in sortedFiles) {
            if (indicator.isCanceled) {
                LOG.info("Generating actions is canceled by user. Done: $completed/${files.size}. With error: $withError")
                break
            }
            LOG.info("Start generating actions for file ${file.path}. Done: $completed/${files.size}. With error: $withError")
            indicator.text2 = file.name
            indicator.fraction = completed.toDouble() / files.size
            try {
                val uast = uastBuilder.build(file)
                generatedActions.add(actionsGenerator.generate(uast))
            } catch (e: Exception) {
                withError++
                LOG.error("Error for file ${file.path}. Message: ${e.message}")
            }
            completed++
            LOG.info("Generating actions for file ${file.path} completed. Done: $completed/${files.size}. With error: $withError")
        }

        return generatedActions.flatten()
    }

    private fun interpretUnderProgress(actions: List<Action>, completionTypes: List<CompletionType>, project: Project, outputDir: String) {
        val task = object : Task.Backgroundable(project, "Interpretation of the generated actions") {
            override fun run(indicator: ProgressIndicator) {
                interpretActions(actions, completionTypes, project, outputDir)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private fun interpretActions(actions: List<Action>, completionTypes: List<CompletionType>, project: Project, outputDir: String) {
        val completionInvoker = DelegationCompletionInvoker(CompletionInvokerImpl(project))
        val metricsEvaluator = MetricsEvaluator.withDefaultMetrics()
        val interpreter = Interpreter(completionInvoker)

        val results = mutableListOf<EvaluationInfo>()
        for (completionType in completionTypes) {
            val files = mutableMapOf<String, FileEvaluationInfo>()
            interpreter.interpret(actions, completionType) { sessions, filePath, fileText ->
                files[filePath] = FileEvaluationInfo(sessions, metricsEvaluator.evaluate(sessions), fileText)
            }
            results.add(EvaluationInfo(completionType.name, files, metricsEvaluator.result()))
        }
        generateReports(outputDir, results)
    }

    private fun generateReports(outputDir: String, results: List<EvaluationInfo>) {
        val reportGenerator = HtmlReportGenerator(outputDir)
        val reportPath = reportGenerator.generateReport(results)
        ApplicationManager.getApplication().invokeAndWait {
            if (OpenBrowserDialog().showAndGet()) BrowserUtil.browse(reportPath)
        }
    }

    private fun getFiles(e: AnActionEvent): Map<Language, Set<VirtualFile>> {
        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return emptyMap()
        val language2files = mutableMapOf<Language, MutableSet<VirtualFile>>()
        for (file in selectedFiles) {
            VfsUtilCore.iterateChildrenRecursively(file, VirtualFileFilter.ALL, object : ContentIterator {
                override fun processFile(fileOrDir: VirtualFile): Boolean {
                    val extension = fileOrDir.extension
                    if (fileOrDir.isDirectory || extension == null) return true

                    val language = Language.resolve(extension)
                    if (language != Language.UNSUPPORTED) {
                        language2files.computeIfAbsent(language) { mutableSetOf() }.add(fileOrDir)
                    }
                    return true
                }
            })
        }
        return language2files
    }

    fun VirtualFile.text(): String {
        return UnixLineEndingInputStream(this.inputStream, false).bufferedReader().use { it.readText() }
    }
}