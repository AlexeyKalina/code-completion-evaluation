package org.jb.cce

import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class HtmlReportGenerator {
    private var reportTitle = "Code Completion Report"

    private val middleCountLookups = 3
    private val goodColor = "#008800"
    private val middleColor = "#999900"
    private val badColor = "#BB0066"

    private val script = HtmlReportGenerator::class.java.getResource("/script.js").readText()
    private val style = HtmlReportGenerator::class.java.getResource("/style.css").readText()
    private val sessionsFileName = "sessions.js"
    private val reportFileName = "report.html"

    fun generate(completions: List<Session>, outputDir: String, filePath: String, text: String) {
        val serializer = SessionSerializer()
        val json = serializer.serialize(completions)
        val file = File(filePath)
        val reportDir = "${file.name}-${Date()}"
        val outputPath = Paths.get(outputDir, reportDir)
        Files.createDirectories(outputPath)
        FileWriter(Paths.get(outputPath.toString(), sessionsFileName).toString()).use { it.write("sessions = '$json'") }

        val report = getHtml(completions, file.name, text)

        FileWriter(Paths.get(outputPath.toString(), reportFileName).toString()).use { it.write(report) }
    }

    private fun getHtml(completions: List<Session>, fileName: String, text: String) : String {
        reportTitle = "Code Completion Report for file <b>$fileName</b>"
        val sb = StringBuilder()
        sb.append("<html>")
        sb.append(getHead())
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

    private fun getHead() =
            """<head><title>$reportTitle</title>
                <script type="text/javascript" src="$sessionsFileName"></script>
                <style>$style</style></head>"""


    private fun prepareCode(text: String, completions: List<Session>) : String {
        val divClosed = "</div>"
        val sb = StringBuilder(text)
        var offset = 0
        for (completion in completions) {
            val divOpened = getDiv(completion)
            sb.insert(completion.offset + offset, divOpened)
            offset += divOpened.length
            sb.insert(completion.offset + completion.completion.length + offset, divClosed)
            offset += divClosed.length
        }
        return sb.toString()
    }

    private fun getDiv(session: Session) : String {
        val color = when {
            !session.lookups.contains(session.completion) -> badColor
            session.lookups.size < middleCountLookups ||
                    session.lookups.subList(0, middleCountLookups).contains(session.completion) -> goodColor
            else -> middleColor
        }
        return "<div class=\"completion\" id=\"${session.id}\" style=\"color: $color; font-weight: bold\">"
    }
}