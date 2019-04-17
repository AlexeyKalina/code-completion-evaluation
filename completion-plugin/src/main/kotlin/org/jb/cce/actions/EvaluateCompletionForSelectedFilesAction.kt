package org.jb.cce.actions

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
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
import java.nio.file.Paths
import java.util.stream.Collectors

class EvaluateCompletionForSelectedFilesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent?) {

        val settingsDialog = CompletionSettingsDialogWrapper()
        val result = settingsDialog.showAndGet()
        if (!result) return

        val project = e?.project ?: return
        val containingFiles = getFiles(project, e)
        containingFiles.forEach { println(it.canonicalPath) }
        val generatedActions = mutableListOf<List<Action>>()
        for (javaFile in containingFiles) {
//            val lexer = Java8Lexer(CharStreams.fromFileName(javaFile.path))
//            val parser = Java8Parser(BufferedTokenStream(lexer))
//            val tree = JavaVisitor().buildUnifiedAst(parser)
            val client = BabelFishClient("bblfsh_client", "0.0.0.0:9432")
            val babelFishUast = client.parse(javaFile.path)
            val tree = BabelFishConverter().convert(babelFishUast, Language.JAVA)
            val strategy = CompletionStrategy(settingsDialog.completionPrefix, settingsDialog.completionStatement, settingsDialog.completionType, settingsDialog.completionContext)
            generatedActions.add(generateActions(javaFile.path, File(javaFile.path).readText(), tree, strategy))
        }
        val completionInvoker = CompletionInvokerImpl(e.project!!)
        val interpretator = Interpretator(completionInvoker)
        val actions = generatedActions.stream()
                .flatMap { l -> l.stream() }
                .collect(Collectors.toList())
        val completions = interpretator.interpret(actions)
        val metricsEvaluator = MetricsEvaluator()
        metricsEvaluator.registerDefaultMetrics()
        metricsEvaluator.evaluate(completions, System.out)
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