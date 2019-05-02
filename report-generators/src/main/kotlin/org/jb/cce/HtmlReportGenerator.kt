package org.jb.cce

import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class HtmlReportGenerator(outputDir: String) {
    companion object {
        private const val middleCountLookups = 3
        private const val goodColor = "#008800"
        private const val middleColor = "#999900"
        private const val badColor = "#BB0066"
        private const val baseDirectory = "reports"
        private const val globalReportName = "index.html"

        private val script = HtmlReportGenerator::class.java.getResource("/script.js").readText()
        private val style = HtmlReportGenerator::class.java.getResource("/style.css").readText()
        private val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    }

    private lateinit var reportTitle: String

    private val _reportsDir = Paths.get(outputDir, baseDirectory, formatter.format(Date()))
    private val reportsDir: String = _reportsDir.toString()

    private val _resourcesDir = Paths.get(reportsDir, "res")
    private val resourcesDir: String = _resourcesDir.toString()

    private data class ResultPaths(val resourcePath: String, val reportPath: String)
    private data class ReportInfo(val reportPath: String, val evaluationResults: String)
    private val references: MutableMap<String, ReportInfo> = mutableMapOf()

    init {
        Files.createDirectories(_resourcesDir)
    }

    fun generate(sessions: List<Session>, filePath: String, text: String, evaluationResults: String) {
        val serializer = SessionSerializer()
        val json = serializer.serialize(sessions)
        val file = File(filePath)
        val (resourcePath, reportPath) = getPaths(file.name)
        FileWriter(resourcePath).use { it.write("sessions = '$json'") }
        val report = getHtml(sessions, file.name, resourcePath, text)
        FileWriter(reportPath).use { it.write(report) }
        references[filePath] = ReportInfo(reportPath, evaluationResults)
    }

    fun generateGlobalReport(evaluationResults: String): String {
        val sb = StringBuilder()
        reportTitle = "Code Completion Report"
        sb.appendln("<html><head><title>$reportTitle</title></head><body><h1>$reportTitle</h1>")
        sb.appendln(evaluationResults)
        sb.appendln("<h3>Reports for files:</h3><ul>")
        for ((file, info) in references) {
            sb.appendln("<li><a href=\"${info.reportPath}\">$file</a>${info.evaluationResults}</li>")
        }
        sb.appendln("</ul></body></html>")

        val reportPath = Paths.get(reportsDir, globalReportName).toString()
        FileWriter(reportPath).use { it.write(sb.toString()) }
        return reportPath
    }

    private fun getPaths(fileName: String): ResultPaths {
        return if (File(Paths.get(resourcesDir, "$fileName.js").toString()).exists()) {
            val lastIndex = getNewIndex(Paths.get(resourcesDir, fileName).toString())
            ResultPaths(Paths.get(resourcesDir, "$fileName-$lastIndex.js").toString(),
                    Paths.get(reportsDir, "$fileName-$lastIndex.html").toString())
        } else {
            ResultPaths(Paths.get(resourcesDir, "$fileName.js").toString(),
                    Paths.get(reportsDir, "$fileName.html").toString())
        }
    }

    private fun getNewIndex(filePath: String): Int {
        var index = 1
        do {
            index++
            val nextFile = "$filePath-$index.js"
        } while (File(nextFile).exists())
        return index
    }

    private fun getHtml(completions: List<Session>, fileName: String, resourcePath: String, text: String) : String {
        reportTitle = "Code Completion Report for file <b>$fileName</b>"
        val sb = StringBuilder()
        sb.append("<html>")
        sb.append(getHead(resourcePath))
        setBody(sb, text, completions)
        sb.append("</html>")

        return sb.toString()
    }

    private fun setBody(sb: StringBuilder, text: String, completions: List<Session>) {
        sb.append("<body>")
        sb.append("<h1>$reportTitle</h1>")
        sb.append("<pre>")
        sb.append(prepareCode(text, completions))
        sb.append("</pre>")
        sb.append("<script>$script</script>")
        sb.append("</body>")
    }

    private fun getHead(resourcePath: String) =
            """<head><title>$reportTitle</title>
                <script type="text/javascript" src="$resourcePath"></script>
                <style>$style</style></head>"""


    private fun prepareCode(text: String, sessions: List<Session>) : String {
        val divClosed = "</div>"
        val sb = StringBuilder(text)
        var offset = 0
        for (session in sessions) {
            val divOpened = getDiv(session)
            sb.insert(session.offset + offset, divOpened)
            offset += divOpened.length
            sb.insert(session.offset + session.expectedText.length + offset, divClosed)
            offset += divClosed.length
        }
        return sb.toString()
    }

    private fun getDiv(session: Session) : String {
        val color = when {
            !session.lookups.last().suggests.contains(session.expectedText) -> badColor
            session.lookups.last().suggests.size < middleCountLookups ||
                    session.lookups.last().suggests.subList(0, middleCountLookups).contains(session.expectedText) -> goodColor
            else -> middleColor
        }
        return "<div class=\"completion\" id=\"${session.id}\" style=\"color: $color; font-weight: bold\">"
    }
}