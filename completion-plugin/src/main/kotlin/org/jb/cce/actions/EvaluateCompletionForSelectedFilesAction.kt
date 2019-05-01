package org.jb.cce.actions

import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import org.apache.commons.io.input.UnixLineEndingInputStream
import org.jb.cce.*
import org.jb.cce.exceptions.BabelFishClientException
import org.jb.cce.interpretator.CompletionInvokerImpl
import org.jb.cce.metrics.MetricsEvaluator
import java.io.File
import java.util.function.Consumer
import java.util.stream.Collectors

class EvaluateCompletionForSelectedFilesAction : AnAction() {
    private companion object {
        val LOG = Logger.getInstance(EvaluateCompletionForSelectedFilesAction::class.java)
    }
    override fun actionPerformed(e: AnActionEvent) {

        val settingsDialog = CompletionSettingsDialogWrapper()
        val result = settingsDialog.showAndGet()
        if (!result) return

        val project = e.project ?: return
        val containingFiles = getFiles(project, e)
        val strategy = CompletionStrategy(settingsDialog.completionPrefix, settingsDialog.completionStatement, settingsDialog.completionType, settingsDialog.completionContext)

        val actions = generateActions(containingFiles, strategy)
        interpretActions(actions, project, settingsDialog.outputDir)
    }

    private fun generateActions(files: List<VirtualFile>, strategy: CompletionStrategy): List<Action> {
        val client = BabelFishClient()
        val converter = BabelFishConverter()

        val generatedActions = mutableListOf<List<Action>>()
        var completed = 0
        var withError = 0
        for (file in files) {
            val language = Language.resolve(file.extension)
            if (language == Language.ANOTHER) {
                completed++
                LOG.warn("Unsupported language for file ${file.path}. File skipped. Done: $completed/${files.size}. With error: $withError")
                continue
            }

            LOG.info("Start actions generation for file ${file.path}. Done: $completed/${files.size}. With error: $withError")
            val fileText = file.text()
            try {
                val babelFishUast = client.parse(fileText, language)
                val tree = converter.convert(babelFishUast, language)
                generatedActions.add(generateActions(file.path, fileText, tree, strategy))
            } catch (e: BabelFishClientException) {
                withError++
                LOG.error("Error for file ${file.path}. Message: ${e.message}")
            }
            completed++
            LOG.info("Actions generation for file ${file.path} completed. Done: $completed/${files.size}. With error: $withError")
        }

        return generatedActions.stream()
                .flatMap { l -> l.stream() }
                .collect(Collectors.toList())
    }

    private fun interpretActions(actions: List<Action>, project: Project, outputDir: String) {
        val completionInvoker = CompletionInvokerImpl(project)
        val reportGenerator = HtmlReportGenerator()
        val metricsEvaluator = MetricsEvaluator()
        metricsEvaluator.registerDefaultMetrics()

        val invokeLaterScheduler = Consumer<Runnable> { ApplicationManager.getApplication().invokeLater(it) }
        val interpretator = Interpretator(completionInvoker, invokeLaterScheduler)
        interpretator.interpret(actions, Consumer { (sessions, filePath, text) ->
            metricsEvaluator.evaluate(sessions, filePath, System.out)
            reportGenerator.generate(sessions, outputDir, filePath, text)
        }, Runnable {
            metricsEvaluator.printResult(System.out)
            if (OpenFolderDialogWrapper().showAndGet()) ShowFilePathAction.openDirectory(File(outputDir))
        })
    }

    private fun getFiles(project: Project, e: AnActionEvent): List<VirtualFile> {
        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return emptyList()
        val files = mutableListOf<VirtualFile>()
        for (it in selectedFiles) {
            VfsUtilCore.iterateChildrenRecursively(it,  VirtualFileFilter.ALL, object: ContentIterator {
                override fun processFile(fileOrDir: VirtualFile): Boolean {
                    if (!fileOrDir.isDirectory) files.add(fileOrDir)
                    return true
                }
            })
        }
        return files.distinct()
    }

    private fun VirtualFile.text(): String {
        return UnixLineEndingInputStream(this.inputStream, false).bufferedReader().use { it.readText() }
    }
}