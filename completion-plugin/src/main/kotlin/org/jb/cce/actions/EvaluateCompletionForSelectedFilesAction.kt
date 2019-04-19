package org.jb.cce.actions

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScopes
import org.antlr.v4.runtime.BufferedTokenStream
import org.antlr.v4.runtime.CharStreams
import org.jb.cce.*
import org.jb.cce.interpretator.CompletionInvokerImpl
import org.jb.cce.metrics.*
import org.jb.cce.uast.FileNode
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Paths
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.stream.Collectors

class EvaluateCompletionForSelectedFilesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {

        val settingsDialog = CompletionSettingsDialogWrapper()
        val result = settingsDialog.showAndGet()
        if (!result) return

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
        val client = BabelFishClient("0.0.0.0:9432")
        for (javaFile in containingFiles) {
            val babelFishUast = client.parse(javaFile.path)
            val tree = converter.convert(babelFishUast, Language.JAVA)
            val fileText = FileReader(javaFile.path).use { it.readText() }

            generatedActions.add(generateActions(javaFile.path, fileText, tree, strategy))
        }

        val invokeLaterScheduler = Consumer<Runnable> { ApplicationManager.getApplication().invokeLater(it) }
        val interpretator = Interpretator(completionInvoker, invokeLaterScheduler)
        interpretator.interpret(generatedActions.stream()
                .flatMap { l -> l.stream() }
                .collect(Collectors.toList()), Consumer { (completions, filePath, text) ->
            metricsEvaluator.evaluate(completions, filePath, System.out)
            reportGenerator.generate(completions, settingsDialog.outputDir, filePath, text)
        }, Runnable { metricsEvaluator.printResult(System.out) })
    }

    private fun getFiles(project: Project, e: AnActionEvent): Collection<VirtualFile> {
        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return emptyList()
        fun files(file: VirtualFile): Collection<VirtualFile> {
            val psiDirectory = PsiManager.getInstance(project).findDirectory(file) ?: return listOf(file)
            return FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScopes.directoryScope(psiDirectory, true))
        }

        return selectedFiles.flatMap { files(it) }.distinct().toList()
    }
}