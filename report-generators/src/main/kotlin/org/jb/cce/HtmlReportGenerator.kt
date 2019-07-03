package org.jb.cce

import org.jb.cce.metrics.MetricInfo
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
        private const val globalReportName = "index.html"
        private const val tabulatorScript = "/tabulator.min.js"
        private const val tabulatorStyle = "/tabulator.min.css"

        private val script = HtmlReportGenerator::class.java.getResource("/script.js").readText()
        private val style = HtmlReportGenerator::class.java.getResource("/style.css").readText()
        private val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    }

    private lateinit var reportTitle: String

    private val reportsDir: String = Paths.get(outputDir, formatter.format(Date())).toString()
    private val _resourcesDir = Paths.get(reportsDir, "res")
    private val resourcesDir: String = _resourcesDir.toString()

    private data class ResultPaths(val resourcePath: String, val reportPath: String)
    private data class ReportInfo(val reportPath: String, val evaluationResults: List<FileEvaluationInfo>)
    private val references: MutableMap<String, ReportInfo> = mutableMapOf()

    init {
        Files.createDirectories(_resourcesDir)
        Files.copy(HtmlReportGenerator::class.java.getResourceAsStream(tabulatorStyle), Paths.get(_resourcesDir.toString(), tabulatorStyle))
        Files.copy(HtmlReportGenerator::class.java.getResourceAsStream(tabulatorScript), Paths.get(_resourcesDir.toString(), tabulatorScript))
    }

    fun generateReport(evaluationResults: List<EvaluationInfo>): String {
        generateFileReports(evaluationResults)
        return generateGlobalReport(evaluationResults)
    }

    private fun generateFileReports(evaluationResults: List<EvaluationInfo>) {
        if (evaluationResults.isEmpty()) return

        val serializer = SessionSerializer()
        for (filePath in evaluationResults.flatMap { it.filesInfo.keys }.distinct()) {
            val sessions = evaluationResults.map { it.filesInfo[filePath]?.sessions ?: listOf() }
            val json = serializer.serialize(sessions.flatten())
            val file = File(filePath)
            val (resourcePath, reportPath) = getPaths(file.name)
            FileWriter(resourcePath).use { it.write("sessions = '$json'") }
            val report = getHtml(sessions, file.name, resourcePath, file.readText())
            FileWriter(reportPath).use { it.write(report) }
            references[filePath] = ReportInfo(reportPath, evaluationResults.map { it.filesInfo.getValue(filePath) })//evaluationResults)
        }
    }

    private fun generateGlobalReport(evaluationResults: List<EvaluationInfo>): String {
        val sb = StringBuilder()
        reportTitle = "Code Completion Report"
        sb.appendln("<html><head><title>$reportTitle</title>")
        sb.appendln("<script src=\"res/tabulator.min.js\"></script>")
        sb.appendln("<link href=\"res/tabulator.min.css\" rel=\"stylesheet\"></head>")
        sb.appendln("<body><h1>$reportTitle</h1>")
        sb.appendln(getMetricsTable(evaluationResults))
        sb.appendln("<script>var table = new Tabulator(\"#metrics-table\", {layout:\"fitColumns\"});</script></body></html>")
        val reportPath = Paths.get(reportsDir, globalReportName).toString()
        FileWriter(reportPath).use { it.write(sb.toString()) }
        return reportPath
    }

    private fun getPaths(fileName: String): ResultPaths {
        return if (Files.exists(Paths.get(resourcesDir, "$fileName.js"))) {
            return getNextFilePaths(Paths.get(resourcesDir, fileName).toString())
        } else {
            ResultPaths(Paths.get(resourcesDir, "$fileName.js").toString(),
                    Paths.get(reportsDir, "$fileName.html").toString())
        }
    }

    private fun getNextFilePaths(filePath: String): ResultPaths {
        var index = 1
        do {
            index++
            val nextFile = "$filePath-$index.js"
        } while (File(nextFile).exists())
        return ResultPaths("$filePath-$index.js", "$filePath-$index.html")
    }

    private fun getHtml(sessions: List<List<Session>>, fileName: String, resourcePath: String, text: String) : String {
        reportTitle = "Code Completion Report for file <b>$fileName</b>"
        val sb = StringBuilder()
        sb.appendln("<html>")
        sb.appendln(getHead(resourcePath))
        setBody(sb, text, sessions)
        sb.appendln("</html>")

        return sb.toString()
    }

    private fun setBody(sb: StringBuilder, text: String, sessions: List<List<Session>>) {
        sb.appendln("<body>")
        sb.appendln("<h1>$reportTitle</h1>")
        sb.appendln("<pre>")
        sb.appendln(prepareCode(text, sessions))
        sb.appendln("</pre>")
        sb.appendln("<script>$script</script>")
        sb.appendln("</body>")
    }

    private fun getHead(resourcePath: String) =
            """<head><title>$reportTitle</title>
                <script type="text/javascript" src="$resourcePath"></script>
                <style>$style</style></head>"""

    private fun prepareCode(text: String, _sessions: List<List<Session>>) : String {
        if (_sessions.isEmpty() || _sessions.all { it.isEmpty() }) return text

        val sessions = _sessions.filterNot { it.isEmpty() }
        val sb = StringBuilder(text)
        var offset = 0
        val delimiter = "&int;"
        for (i in sessions.first().indices) {
            val center = sessions.first()[i].expectedText.length / sessions.size
            var curOffset = sessions.first()[i].offset
            for (j in 0 until sessions.lastIndex) {
                offset = writeDiv(sessions[j][i], sb, offset, curOffset, curOffset + center)
                curOffset += center
                sb.insert(offset + curOffset, delimiter)
                offset += delimiter.length
            }
            offset = writeDiv(sessions.last()[i], sb, offset, curOffset,
                    sessions.first()[i].offset + sessions.first()[i].expectedText.length)
        }
        return sb.toString()
    }

    private fun writeDiv(session: Session, sb: StringBuilder, offset_: Int, startPosition: Int, endPosition: Int) : Int {
        val opened = "<div class=\"completion\" id=\"${session.id}\" style=\"color: ${getColor(session)}; font-weight: bold\">"
        val closed = "</div>"
        var offset = offset_
        sb.insert(offset + startPosition, opened)
        offset += opened.length
        sb.insert(offset + endPosition, closed)
        offset += closed.length
        return offset
    }

    private fun getColor(session: Session): String {
        return when {
            !session.lookups.last().suggests.contains(session.expectedText) -> badColor
            session.lookups.last().suggests.size < middleCountLookups ||
                    session.lookups.last().suggests.subList(0, middleCountLookups).contains(session.expectedText) -> goodColor
            else -> middleColor
        }
    }

    private fun getMetricsTable(evaluationResults: List<EvaluationInfo>): String {
        val headerBuilder = StringBuilder()
        val contentBuilder = StringBuilder()

        headerBuilder.appendln("<th tabulator-formatter=\"html\">File Report</th>")
        for (metric in evaluationResults.flatMap { res -> res.metrics.map { Pair(it.name, res.evaluationType) } }.sortedBy { it.first })
            headerBuilder.appendln("<th>${metric.first} ${metric.second}</th>")

        for (filePath in evaluationResults.flatMap { it.filesInfo.keys }.distinct())
            writeRow(contentBuilder, "<a href=\"${references[filePath]!!.reportPath}\">${File(filePath).name}</a>",
                evaluationResults.flatMap { it.filesInfo[filePath]?.metrics ?: it.metrics.map { MetricInfo(it.name, null)} }.sortedBy { it.name })

        writeRow(contentBuilder, "Summary", evaluationResults.flatMap { it.metrics }.sortedBy { it.name })
        contentBuilder.appendln("</tr>")

        return """
            <table id="metrics-table">
                <thead>
                    <tr>
                        $headerBuilder
                    </tr>
                </thead>
                <tbody>
                    $contentBuilder
                </tbody>
            </table>
        """
    }

    private fun writeRow(sb: StringBuilder, name: String, metrics: List<MetricInfo>) {
        sb.appendln("<tr><td>$name</td>")
        for (metric in metrics) {
            if (metric.value == null) sb.appendln("<td>----</td>")
            else sb.appendln("<td>%.3f</td>".format(metric.value))
        }
        sb.appendln("</tr>")
    }
}