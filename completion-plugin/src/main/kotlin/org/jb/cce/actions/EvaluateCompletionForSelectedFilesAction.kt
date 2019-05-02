package org.jb.cce.actions

import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.ui.Messages
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

        val settingsDialog = CompletionSettingsDialogWrapper(language2files)
        val result = settingsDialog.showAndGet()
        if (!result) return

        val strategy = CompletionStrategy(settingsDialog.completionPrefix, settingsDialog.completionStatement, settingsDialog.completionType, settingsDialog.completionContext)
        val actions = generateActions(settingsDialog.language, language2files.getValue(settingsDialog.language), strategy)
        interpretActions(actions, project, settingsDialog.outputDir)
    }

    private fun generateActions(language: Language, files: Collection<VirtualFile>, strategy: CompletionStrategy): List<Action> {
        val client = BabelFishClient()
        val converter = BabelFishConverter()
        val sortedFiles = files.sortedBy { f -> f.name }

        val generatedActions = mutableListOf<List<Action>>()
        var completed = 0
        var withError = 0
        for (file in sortedFiles) {
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

        return generatedActions.flatten()
    }

    private fun interpretActions(actions: List<Action>, project: Project, outputDir: String) {
        val completionInvoker = CompletionInvokerImpl(project)
        val reportGenerator = HtmlReportGenerator()
        val metricsEvaluator = MetricsEvaluator.withDefaultMetrics()

        val invokeLaterScheduler = Consumer<Runnable> { ApplicationManager.getApplication().invokeLater(it) }
        val interpreter = Interpreter(completionInvoker, invokeLaterScheduler)
        interpreter.interpret(actions, Consumer { (sessions, filePath, text) ->
            metricsEvaluator.evaluate(sessions, filePath, System.out)
            reportGenerator.generate(sessions, outputDir, filePath, text)
        }, Runnable {
            metricsEvaluator.printResult(System.out)
            if (OpenFolderDialogWrapper().showAndGet()) ShowFilePathAction.openDirectory(File(outputDir))
        })
    }

    private fun getFiles(e: AnActionEvent): Map<Language, Set<VirtualFile>> {
        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return emptyMap()
        val language2files = mutableMapOf<Language, MutableSet<VirtualFile>>()
        for (file in selectedFiles) {
            VfsUtilCore.iterateChildrenRecursively(file,  VirtualFileFilter.ALL, object: ContentIterator {
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

    private fun VirtualFile.text(): String {
        return UnixLineEndingInputStream(this.inputStream, false).bufferedReader().use { it.readText() }
    }
}