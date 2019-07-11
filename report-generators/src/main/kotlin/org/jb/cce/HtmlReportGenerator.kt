package org.jb.cce

import org.jb.cce.info.FileEvaluationInfo
import org.jb.cce.info.MetricsEvaluationInfo
import org.jb.cce.info.SessionsEvaluationInfo
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
        private const val absentColor = "#70AAFF"
        private const val globalReportName = "index.html"
        private const val tabulatorScript = "/tabulator.min.js"
        private const val tabulatorStyle = "/tabulator.min.css"

        private val script = HtmlReportGenerator::class.java.getResource("/script.js").readText()
        private val style = HtmlReportGenerator::class.java.getResource("/style.css").readText()
        private val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")

        private val serializer = SessionSerializer()
    }

    private lateinit var reportTitle: String

    private val reportsDir: String = Paths.get(outputDir, formatter.format(Date())).toString()
    private val resourcesDir = Paths.get(reportsDir, "res")
    private val resultsDir = Paths.get(reportsDir, "data")

    private data class ResultPaths(val resourcePath: String, val reportPath: String)
    private data class ReportInfo(val reportPath: String, val sessions: List<FileEvaluationInfo<Session>>)
    private val references: MutableMap<String, ReportInfo> = mutableMapOf()

    init {
        Files.createDirectories(resourcesDir)
        Files.createDirectories(resultsDir)
        Files.copy(HtmlReportGenerator::class.java.getResourceAsStream(tabulatorStyle), Paths.get(resourcesDir.toString(), tabulatorStyle))
        Files.copy(HtmlReportGenerator::class.java.getResourceAsStream(tabulatorScript), Paths.get(resourcesDir.toString(), tabulatorScript))
    }

    fun generateReport(sessions: List<SessionsEvaluationInfo>, metrics: List<MetricsEvaluationInfo>): String {
        saveEvaluationResults(sessions)
        generateFileReports(sessions)
        return generateGlobalReport(metrics)
    }

    private fun generateFileReports(evaluationResults: List<SessionsEvaluationInfo>) {
        if (evaluationResults.isEmpty()) return

        for (filePath in evaluationResults.flatMap { it.sessions.map { it.filePath } }.distinct()) {
            val sessions = evaluationResults.map { it.sessions.find { it.filePath == filePath }?.results ?: listOf() }
            val json = serializer.serialize(sessions.flatten())
            val file = File(filePath)
            val (resourcePath, reportPath) = getPaths(file.name)
            FileWriter(resourcePath).use { it.write("sessions = '$json'") }
            val report = getHtml(sessions, file.name, resourcePath,
                    evaluationResults.mapNotNull { it.sessions.find { it.filePath == filePath }?.text }.first() )
            FileWriter(reportPath).use { it.write(report) }
            references[filePath] = ReportInfo(reportPath, evaluationResults.mapNotNull { it.sessions.find { it.filePath == filePath } })
        }
    }

    private fun generateGlobalReport(evaluationResults: List<MetricsEvaluationInfo>): String {
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

    private fun saveEvaluationResults(evaluationResults: List<SessionsEvaluationInfo>) {
        for (results in evaluationResults) {
            val json = serializer.serialize(results)
            val dataPath = Paths.get(resultsDir.toString(), "${results.info.evaluationType}.json").toString()
            FileWriter(dataPath).use { it.write(json) }
        }
    }

    private fun getPaths(fileName: String): ResultPaths {
        return if (Files.exists(Paths.get(resourcesDir.toString(), "$fileName.js"))) {
            return getNextFilePaths(Paths.get(resourcesDir.toString(), fileName).toString())
        } else {
            ResultPaths(Paths.get(resourcesDir.toString(), "$fileName.js").toString(),
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
        for (session in sessions.flatten().distinctBy { it.offset }) {
            val center = session.expectedText.length / sessions.size
            var curOffset = session.offset
            for (j in 0 until sessions.lastIndex) {
                offset = writeDiv(sessions[j].find { it.offset == session.offset }, sb, offset, curOffset, curOffset + center)
                curOffset += center
                sb.insert(offset + curOffset, delimiter)
                offset += delimiter.length
            }
            offset = writeDiv(sessions.last().find { it.offset == session.offset }, sb, offset, curOffset,
                    session.offset + session.expectedText.length)
        }
        return sb.toString()
    }

    private fun writeDiv(session: Session?, sb: StringBuilder, offset_: Int, startPosition: Int, endPosition: Int) : Int {
        val opened = "<div class=\"completion\" id=\"${session?.id}\" style=\"color: ${getColor(session)}; font-weight: bold\">"
        val closed = "</div>"
        var offset = offset_
        sb.insert(offset + startPosition, opened)
        offset += opened.length
        sb.insert(offset + endPosition, closed)
        offset += closed.length
        return offset
    }

    private fun getColor(session: Session?): String {
        return when {
            session == null -> absentColor
            !session.lookups.last().suggests.any{ it.text == session.expectedText } -> badColor
            session.lookups.last().suggests.size < middleCountLookups ||
                    session.lookups.last().suggests.subList(0, middleCountLookups).any{ it.text == session.expectedText } -> goodColor
            else -> middleColor
        }
    }

    private fun getMetricsTable(evaluationResults: List<MetricsEvaluationInfo>): String {
        val headerBuilder = StringBuilder()
        val contentBuilder = StringBuilder()

        headerBuilder.appendln("<th tabulator-formatter=\"html\">File Report</th>")
        for (metric in evaluationResults.flatMap { res -> res.globalMetrics.map { Pair(it.name, res.info.evaluationType) } }.sortedBy { it.first })
            headerBuilder.appendln("<th>${metric.first} ${metric.second}</th>")

        for (filePath in evaluationResults.flatMap { it.fileMetrics.map { it.filePath } }.distinct())
            writeRow(contentBuilder, "<a href=\"${references[filePath]!!.reportPath}\">${File(filePath).name}</a>",
                evaluationResults.flatMap { it.fileMetrics.find { it.filePath == filePath }?.results ?:
                it.globalMetrics.map { MetricInfo(it.name, null)} }.sortedBy { it.name })

        writeRow(contentBuilder, "Summary", evaluationResults.flatMap { it.globalMetrics }.sortedBy { it.name })
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