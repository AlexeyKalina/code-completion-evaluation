package org.jb.cce.actions

import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScopes
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

        val outputDir = settingsDialog.outputDir
        val project = e.project ?: return
        val containingFiles = getFiles(project, e)

        val client = BabelFishClient()
        val converter = BabelFishConverter()
        val completionInvoker = CompletionInvokerImpl(e.project!!)

        val reportGenerator = HtmlReportGenerator()
        val metricsEvaluator = MetricsEvaluator()
        metricsEvaluator.registerDefaultMetrics()
        val strategy = CompletionStrategy(settingsDialog.completionPrefix, settingsDialog.completionStatement, settingsDialog.completionType, settingsDialog.completionContext)

        val generatedActions = mutableListOf<List<Action>>()
        var completed = 0
        var withError = 0
        for (javaFile in containingFiles) {
            LOG.info("Start actions generation for file ${javaFile.path}. Done: $completed/${containingFiles.size}. With error: $withError")
            val fileText = javaFile.text()
            try {
                val babelFishUast = client.parse(fileText, Language.JAVA)
                val tree = converter.convert(babelFishUast, Language.JAVA)
                generatedActions.add(generateActions(javaFile.path, fileText, tree, strategy))
            } catch (e: BabelFishClientException) {
                withError++
                LOG.error("Error for file ${javaFile.path}. Message: ${e.message}")
            }
            completed++
            LOG.info("Actions generation for file ${javaFile.path} completed. Done: $completed/${containingFiles.size}. With error: $withError")
        }

        val invokeLaterScheduler = Consumer<Runnable> { ApplicationManager.getApplication().invokeLater(it) }
        val interpretator = Interpretator(completionInvoker, invokeLaterScheduler)
        interpretator.interpret(generatedActions.stream()
                .flatMap { l -> l.stream() }
                .collect(Collectors.toList()), Consumer { (sessions, filePath, text) ->
            metricsEvaluator.evaluate(sessions, filePath, System.out)
            reportGenerator.generate(sessions, outputDir, filePath, text)
        }, Runnable {
            metricsEvaluator.printResult(System.out)
            if (OpenFolderDialogWrapper().showAndGet()) ShowFilePathAction.openDirectory(File(outputDir))
        })
    }

    private fun getFiles(project: Project, e: AnActionEvent): Collection<VirtualFile> {
        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return emptyList()
        fun files(file: VirtualFile): Collection<VirtualFile> {
            val psiDirectory = PsiManager.getInstance(project).findDirectory(file) ?: return listOf(file)
            return FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScopes.directoryScope(psiDirectory, true))
        }

        return selectedFiles.flatMap { files(it) }.distinct().toList()
    }

    private fun VirtualFile.text(): String {
        return UnixLineEndingInputStream(this.inputStream, false).bufferedReader().use { it.readText() }
    }
}