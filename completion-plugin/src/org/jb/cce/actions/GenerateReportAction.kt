package org.jb.cce.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jb.cce.HtmlReportGenerator
import org.jb.cce.Session
import org.jb.cce.SessionSerializer
import org.jb.cce.generateReportUnderProgress
import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.info.SessionsEvaluationInfo
import java.nio.file.Paths

class GenerateReportAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = getFiles(e)

        val sessionsInfo = mutableListOf<SessionsEvaluationInfo>()
        val serializer = SessionSerializer()
        for (configFile in files) {
            val config = serializer.deserializeConfig(VfsUtil.loadText(configFile))
            val sessions = mutableListOf<FileEvaluationInfo<Session>>()
            val sessionsDir = configFile.parent.children.find { it.isDirectory } ?: throw IllegalStateException("No sessions directory")
            for (file in sessionsDir.children) {
                sessions.add(serializer.deserialize(VfsUtil.loadText(file)))
            }
            sessionsInfo.add(SessionsEvaluationInfo(sessions, config))
        }

        val properties = PropertiesComponent.getInstance(project)
        val outputDir = properties.getValue(CompletionSettingsDialog.workspaceDirProperty) ?:
            Paths.get(project.basePath ?: "", CompletionSettingsDialog.completionEvaluationDir).toString()

        val reportGenerator = HtmlReportGenerator(outputDir)
        generateReportUnderProgress(sessionsInfo, emptyList(), reportGenerator, project, false)
    }

    override fun update(e: AnActionEvent) {
        val files = getFiles(e)
        e.presentation.isEnabled = files.isNotEmpty() && files.all { it.extension == "json" }
    }

    private fun getFiles(e: AnActionEvent) : List<VirtualFile> = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList() ?: emptyList()
}