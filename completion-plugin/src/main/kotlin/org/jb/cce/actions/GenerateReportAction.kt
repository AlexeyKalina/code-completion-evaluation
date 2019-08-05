package org.jb.cce.actions

import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.HtmlReportGenerator
import org.jb.cce.SessionSerializer
import org.jb.cce.generateReport
import org.jb.cce.info.SessionsEvaluationInfo
import java.nio.file.Paths

class GenerateReportAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = getFiles(e)

        val sessionsInfo = mutableListOf<SessionsEvaluationInfo>()
        val serializer = SessionSerializer()
        for (file in files) {
            sessionsInfo.add(serializer.deserialize(VfsUtil.loadText(file)))
        }

        val properties = PropertiesComponent.getInstance(project)
        val outputDir = properties.getValue(CompletionSettingsDialog.workspaceDirProperty) ?:
            Paths.get(project.basePath ?: "", CompletionSettingsDialog.completionEvaluationDir).toString()

        val reportGenerator = HtmlReportGenerator(outputDir)
        val reportPath = generateReport(reportGenerator, sessionsInfo, emptyList())
        ApplicationManager.getApplication().invokeAndWait {
            if (OpenBrowserDialog().showAndGet()) BrowserUtil.browse(reportPath)
        }
    }

    override fun update(e: AnActionEvent) {
        val files = getFiles(e)
        e.presentation.isEnabled = files.isNotEmpty() && files.all { it.extension == "json" }
    }

    private fun getFiles(e: AnActionEvent) : List<VirtualFile> = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList() ?: emptyList()
}